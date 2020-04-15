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

package org.springframework.cloud.stream.binder.kafka.streams.integration;

import java.util.Arrays;
import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Serialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.binder.kafka.streams.annotations.KafkaStreamsProcessor;
import org.springframework.cloud.stream.binder.kafka.streams.properties.KafkaStreamsApplicationSupportProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.rule.EmbeddedKafkaRule;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Soby Chacko
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@DirtiesContext
public abstract class KafkaStreamsNativeEncodingDecodingTests {

	@ClassRule
	public static EmbeddedKafkaRule embeddedKafkaRule = new EmbeddedKafkaRule(1, true, "counts");

	private static EmbeddedKafkaBroker embeddedKafka = embeddedKafkaRule.getEmbeddedKafka();

	@SpyBean
	org.springframework.cloud.stream.binder.kafka.streams.KafkaStreamsMessageConversionDelegate KafkaStreamsMessageConversionDelegate;

	private static Consumer<String, String> consumer;

	@BeforeClass
	public static void setUp() throws Exception {
		System.setProperty("spring.cloud.stream.kafka.streams.binder.brokers", embeddedKafka.getBrokersAsString());
		System.setProperty("spring.cloud.stream.kafka.streams.binder.zkNodes", embeddedKafka.getZookeeperConnectionString());

		System.setProperty("server.port","0");
		System.setProperty("spring.jmx.enabled","false");

		Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("group", "false", embeddedKafka);
		consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
		consumer = cf.createConsumer();
		embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "counts");
	}

	@AfterClass
	public static void tearDown() {
		consumer.close();
	}

	@SpringBootTest(properties = {
			"spring.cloud.stream.bindings.input.consumer.useNativeDecoding=true",
			"spring.cloud.stream.bindings.output.producer.useNativeEncoding=true",
			"spring.cloud.stream.kafka.streams.bindings.input.consumer.applicationId=NativeEncodingDecodingEnabledTests-abc"
	},
			webEnvironment= SpringBootTest.WebEnvironment.NONE
			)
	public static class NativeEncodingDecodingEnabledTests extends KafkaStreamsNativeEncodingDecodingTests {

		@Test
		public void test() throws Exception {
			Map<String, Object> senderProps = KafkaTestUtils.producerProps(embeddedKafka);
			DefaultKafkaProducerFactory<Integer, String> pf = new DefaultKafkaProducerFactory<>(senderProps);
			KafkaTemplate<Integer, String> template = new KafkaTemplate<>(pf, true);
			template.setDefaultTopic("words");
			template.sendDefault("foobar");
			ConsumerRecord<String, String> cr = KafkaTestUtils.getSingleRecord(consumer, "counts");
			assertThat(cr.value().equals("Count for foobar : 1")).isTrue();

			verify(KafkaStreamsMessageConversionDelegate, never()).serializeOnOutbound(any(KStream.class));
			verify(KafkaStreamsMessageConversionDelegate, never()).deserializeOnInbound(any(Class.class), any(KStream.class));
		}
	}

	@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.NONE,
	properties = "spring.cloud.stream.kafka.streams.bindings.input.consumer.applicationId=NativeEncodingDecodingEnabledTests-xyz")
	public static class NativeEncodingDecodingDisabledTests extends KafkaStreamsNativeEncodingDecodingTests {

		@Test
		public void test() throws Exception {
			Map<String, Object> senderProps = KafkaTestUtils.producerProps(embeddedKafka);
			DefaultKafkaProducerFactory<Integer, String> pf = new DefaultKafkaProducerFactory<>(senderProps);
			KafkaTemplate<Integer, String> template = new KafkaTemplate<>(pf, true);
			template.setDefaultTopic("words");
			template.sendDefault("foobar");
			ConsumerRecord<String, String> cr = KafkaTestUtils.getSingleRecord(consumer, "counts");
			assertThat(cr.value().equals("Count for foobar : 1")).isTrue();

			verify(KafkaStreamsMessageConversionDelegate).serializeOnOutbound(any(KStream.class));
			verify(KafkaStreamsMessageConversionDelegate).deserializeOnInbound(any(Class.class), any(KStream.class));
		}
	}

	@EnableBinding(KafkaStreamsProcessor.class)
	@EnableAutoConfiguration
	@PropertySource("classpath:/org/springframework/cloud/stream/binder/kstream/integTest-1.properties")
	@EnableConfigurationProperties(KafkaStreamsApplicationSupportProperties.class)
	public static class WordCountProcessorApplication {

		@Autowired
		private TimeWindows timeWindows;

		@StreamListener("input")
		@SendTo("output")
		public KStream<?, String> process(KStream<Object, String> input) {

			return input
					.flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))
					.map((key, value) -> new KeyValue<>(value, value))
					.groupByKey(Serialized.with(Serdes.String(), Serdes.String()))
					.windowedBy(timeWindows)
					.count(Materialized.as("foo-WordCounts-x"))
					.toStream()
					.map((key, value) -> new KeyValue<>(null, "Count for " + key.key() + " : " + value));
		}
	}

}
