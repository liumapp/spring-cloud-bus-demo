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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.streams.errors.DeserializationExceptionHandler;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.internals.ProcessorContextImpl;
import org.apache.kafka.streams.processor.internals.StreamTask;

import org.springframework.util.ReflectionUtils;

/**
 * Custom implementation for {@link DeserializationExceptionHandler} that sends the records
 * in error to a DLQ topic, then continue stream processing on new records.
 *
 * @author Soby Chacko
 * @since 2.0.0
 */
public class SendToDlqAndContinue implements DeserializationExceptionHandler {

	/**
	 * Key used for DLQ dispatchers.
	 */
	public static final String KAFKA_STREAMS_DLQ_DISPATCHERS = "spring.cloud.stream.kafka.streams.dlq.dispatchers";

	/**
	 * DLQ dispatcher per topic in the application context. The key here is not the actual DLQ topic
	 * but the incoming topic that caused the error.
	 */
	private Map<String, KafkaStreamsDlqDispatch> dlqDispatchers = new HashMap<>();

	/**
	 * For a given topic, send the key/value record to DLQ topic.
	 *
	 * @param topic incoming topic that caused the error
	 * @param key to send
	 * @param value to send
	 * @param partition for the topic where this record should be sent
	 */
	public void sendToDlq(String topic, byte[] key, byte[] value, int partition) {
		KafkaStreamsDlqDispatch kafkaStreamsDlqDispatch = this.dlqDispatchers.get(topic);
		kafkaStreamsDlqDispatch.sendToDlq(key, value, partition);
	}

	@Override
	@SuppressWarnings("unchecked")
	public DeserializationHandlerResponse handle(ProcessorContext context, ConsumerRecord<byte[], byte[]> record, Exception exception) {
		KafkaStreamsDlqDispatch kafkaStreamsDlqDispatch = this.dlqDispatchers.get(record.topic());
		kafkaStreamsDlqDispatch.sendToDlq(record.key(), record.value(), record.partition());
		context.commit();

		// The following conditional block should be reconsidered when we have a solution for this SO problem:
		// https://stackoverflow.com/questions/48470899/kafka-streams-deserialization-handler
		// Currently it seems like when deserialization error happens, there is no commits happening and the
		// following code will use reflection to get access to the underlying KafkaConsumer.
		// It works with Kafka 1.0.0, but there is no guarantee it will work in future versions of kafka as
		// we access private fields by name using reflection, but it is a temporary fix.
		if (context instanceof ProcessorContextImpl) {
			ProcessorContextImpl processorContextImpl = (ProcessorContextImpl) context;
			Field task = ReflectionUtils.findField(ProcessorContextImpl.class, "task");
			ReflectionUtils.makeAccessible(task);
			Object taskField = ReflectionUtils.getField(task, processorContextImpl);

			if (taskField.getClass().isAssignableFrom(StreamTask.class)) {
				StreamTask streamTask = (StreamTask) taskField;
				Field consumer = ReflectionUtils.findField(StreamTask.class, "consumer");
				ReflectionUtils.makeAccessible(consumer);
				Object kafkaConsumerField = ReflectionUtils.getField(consumer, streamTask);
				if (kafkaConsumerField.getClass().isAssignableFrom(KafkaConsumer.class)) {
					KafkaConsumer kafkaConsumer = (KafkaConsumer) kafkaConsumerField;
					final Map<TopicPartition, OffsetAndMetadata> consumedOffsetsAndMetadata = new HashMap<>();
					TopicPartition tp = new TopicPartition(record.topic(), record.partition());
					OffsetAndMetadata oam = new OffsetAndMetadata(record.offset() + 1);
					consumedOffsetsAndMetadata.put(tp, oam);
					kafkaConsumer.commitSync(consumedOffsetsAndMetadata);
				}
			}
		}
		return DeserializationHandlerResponse.CONTINUE;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void configure(Map<String, ?> configs) {
		this.dlqDispatchers = (Map<String, KafkaStreamsDlqDispatch>) configs.get(KAFKA_STREAMS_DLQ_DISPATCHERS);
	}

	void addKStreamDlqDispatch(String topic, KafkaStreamsDlqDispatch kafkaStreamsDlqDispatch) {
		this.dlqDispatchers.put(topic, kafkaStreamsDlqDispatch);
	}

}
