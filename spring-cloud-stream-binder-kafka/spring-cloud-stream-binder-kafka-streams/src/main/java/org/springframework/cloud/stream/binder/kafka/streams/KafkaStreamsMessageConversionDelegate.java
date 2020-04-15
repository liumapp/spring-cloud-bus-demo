/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.binder.kafka.streams;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;

import org.springframework.cloud.stream.binder.kafka.streams.properties.KafkaStreamsBinderConfigurationProperties;
import org.springframework.cloud.stream.converter.CompositeMessageConverterFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Delegate for handling all framework level message conversions inbound and outbound on {@link KStream}.
 * If native encoding is not enabled, then serialization will be performed on outbound messages based
 * on a contentType. Similarly, if native decoding is not enabled, deserialization will be performed on
 * inbound messages based on a contentType. Based on the contentType, a {@link MessageConverter} will
 * be resolved.
 *
 * @author Soby Chacko
 */
public class KafkaStreamsMessageConversionDelegate {

	private static final Log LOG = LogFactory.getLog(KafkaStreamsMessageConversionDelegate.class);

	private static final ThreadLocal<KeyValue<Object, Object>> keyValueThreadLocal = new ThreadLocal<>();

	private final CompositeMessageConverterFactory compositeMessageConverterFactory;

	private final SendToDlqAndContinue sendToDlqAndContinue;

	private final KafkaStreamsBindingInformationCatalogue kstreamBindingInformationCatalogue;

	private final KafkaStreamsBinderConfigurationProperties kstreamBinderConfigurationProperties;

	KafkaStreamsMessageConversionDelegate(CompositeMessageConverterFactory compositeMessageConverterFactory,
										SendToDlqAndContinue sendToDlqAndContinue,
										KafkaStreamsBindingInformationCatalogue kstreamBindingInformationCatalogue,
										KafkaStreamsBinderConfigurationProperties kstreamBinderConfigurationProperties) {
		this.compositeMessageConverterFactory = compositeMessageConverterFactory;
		this.sendToDlqAndContinue = sendToDlqAndContinue;
		this.kstreamBindingInformationCatalogue = kstreamBindingInformationCatalogue;
		this.kstreamBinderConfigurationProperties = kstreamBinderConfigurationProperties;
	}

	/**
	 * Serialize {@link KStream} records on outbound based on contentType.
	 *
	 * @param outboundBindTarget outbound KStream target
	 * @return serialized KStream
	 */
	@SuppressWarnings("rawtypes")
	public KStream serializeOnOutbound(KStream<?, ?> outboundBindTarget) {
		String contentType = this.kstreamBindingInformationCatalogue.getContentType(outboundBindTarget);
		MessageConverter messageConverter = this.compositeMessageConverterFactory.getMessageConverterForAllRegistered();

		return outboundBindTarget.mapValues((v) -> {
			Message<?> message = v instanceof Message<?> ? (Message<?>) v :
					MessageBuilder.withPayload(v).build();
			Map<String, Object> headers = new HashMap<>(message.getHeaders());
			if (!StringUtils.isEmpty(contentType)) {
				headers.put(MessageHeaders.CONTENT_TYPE, contentType);
			}
			MessageHeaders messageHeaders = new MessageHeaders(headers);
			return
					messageConverter.toMessage(message.getPayload(),
							messageHeaders).getPayload();
		});
	}

	/**
	 * Deserialize incoming {@link KStream} based on content type.
	 *
	 * @param valueClass on KStream value
	 * @param bindingTarget inbound KStream target
	 * @return deserialized KStream
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public KStream deserializeOnInbound(Class<?> valueClass, KStream<?, ?> bindingTarget) {
		MessageConverter messageConverter = this.compositeMessageConverterFactory.getMessageConverterForAllRegistered();
		final PerRecordContentTypeHolder perRecordContentTypeHolder = new PerRecordContentTypeHolder();

		resolvePerRecordContentType(bindingTarget, perRecordContentTypeHolder);

		//Deserialize using a branching strategy
		KStream<?, ?>[] branch = bindingTarget.branch(
			//First filter where the message is converted and return true if everything went well, return false otherwise.
			(o, o2) -> {
				boolean isValidRecord = false;

				try {
					//if the record is a tombstone, ignore and exit from processing further.
					if (o2 != null) {
						if (o2 instanceof Message || o2 instanceof String || o2 instanceof byte[]) {
							Message<?> m1 = null;
							if (o2 instanceof Message) {
								m1 = perRecordContentTypeHolder.contentType != null
										? MessageBuilder.fromMessage((Message<?>) o2).setHeader(MessageHeaders.CONTENT_TYPE,
										perRecordContentTypeHolder.contentType).build() : (Message<?>) o2;
							}
							else {
								m1 = perRecordContentTypeHolder.contentType != null ? MessageBuilder.withPayload(o2)
										.setHeader(MessageHeaders.CONTENT_TYPE, perRecordContentTypeHolder.contentType).build() : MessageBuilder.withPayload(o2).build();
							}
							convertAndSetMessage(o, valueClass, messageConverter, m1);
						}
						else {
							keyValueThreadLocal.set(new KeyValue<>(o, o2));
						}
						isValidRecord = true;
					}
					else {
						LOG.info("Received a tombstone record. This will be skipped from further processing.");
					}
				}
				catch (Exception e) {
					LOG.warn("Deserialization has failed. This will be skipped from further processing.", e);
					//pass through
				}
				return isValidRecord;
			},
			//second filter that catches any messages for which an exception thrown in the first filter above.
			(k, v) -> true
		);
		//process errors from the second filter in the branch above.
		processErrorFromDeserialization(bindingTarget, branch[1]);

		//first branch above is the branch where the messages are converted, let it go through further processing.
		return branch[0].mapValues((o2) -> {
			Object objectValue = keyValueThreadLocal.get().value;
			keyValueThreadLocal.remove();
			return objectValue;
		});
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void resolvePerRecordContentType(KStream<?, ?> outboundBindTarget, PerRecordContentTypeHolder perRecordContentTypeHolder) {
		outboundBindTarget.process(() -> new Processor() {

			ProcessorContext context;

			@Override
			public void init(ProcessorContext context) {
				this.context = context;
			}

			@Override
			public void process(Object key, Object value) {
				final Headers headers = this.context.headers();
				final Iterable<Header> contentTypes = headers.headers(MessageHeaders.CONTENT_TYPE);
				if (contentTypes != null && contentTypes.iterator().hasNext()) {
					final String contentType = new String(contentTypes.iterator().next().value());
					//remove leading and trailing quotes
					final String cleanContentType = StringUtils.replace(contentType, "\"", "");
					perRecordContentTypeHolder.setContentType(cleanContentType);
				}
			}

			@Override
			public void close() {

			}
		});
	}

	private void convertAndSetMessage(Object o, Class<?> valueClass, MessageConverter messageConverter, Message<?> msg) {
		Object result = valueClass.isAssignableFrom(msg.getPayload().getClass())
				? msg.getPayload() : messageConverter.fromMessage(msg, valueClass);

		Assert.notNull(result, "Failed to convert message " + msg);

		keyValueThreadLocal.set(new KeyValue<>(o, result));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void processErrorFromDeserialization(KStream<?, ?> bindingTarget, KStream<?, ?> branch) {
		branch.process(() -> new Processor() {
			ProcessorContext context;

			@Override
			public void init(ProcessorContext context) {
				this.context = context;
			}

			@Override
			public void process(Object o, Object o2) {
				//Only continue if the record was not a tombstone.
				if (o2 != null) {
					if (KafkaStreamsMessageConversionDelegate.this.kstreamBindingInformationCatalogue.isDlqEnabled(bindingTarget)) {
						String destination = this.context.topic();
						if (o2 instanceof Message) {
							Message message = (Message) o2;
							KafkaStreamsMessageConversionDelegate.this.sendToDlqAndContinue.sendToDlq(destination, (byte[]) o, (byte[]) message.getPayload(), this.context.partition());
						}
						else {
							KafkaStreamsMessageConversionDelegate.this.sendToDlqAndContinue.sendToDlq(destination, (byte[]) o, (byte[]) o2, this.context.partition());
						}
					}
					else if (KafkaStreamsMessageConversionDelegate.this.kstreamBinderConfigurationProperties.getSerdeError() == KafkaStreamsBinderConfigurationProperties.SerdeError.logAndFail) {
						throw new IllegalStateException("Inbound deserialization failed. Stopping further processing of records.");
					}
					else if (KafkaStreamsMessageConversionDelegate.this.kstreamBinderConfigurationProperties.getSerdeError() == KafkaStreamsBinderConfigurationProperties.SerdeError.logAndContinue) {
						//quietly passing through. No action needed, this is similar to log and continue.
						LOG.error("Inbound deserialization failed. Skipping this record and continuing.");
					}
				}
			}

			@Override
			public void close() {

			}
		});
	}

	private static class PerRecordContentTypeHolder {

		String contentType;

		void setContentType(String contentType) {
			this.contentType = contentType;
		}
	}
}
