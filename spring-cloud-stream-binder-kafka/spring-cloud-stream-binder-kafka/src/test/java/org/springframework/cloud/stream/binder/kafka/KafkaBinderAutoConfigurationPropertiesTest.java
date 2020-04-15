/*
 * Copyright 2016-2018 the original author or authors.
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

package org.springframework.cloud.stream.binder.kafka;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.kafka.config.KafkaBinderConfiguration;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaConsumerProperties;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaProducerProperties;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ReflectionUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Ilayaperumal Gopinathan
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {KafkaBinderConfiguration.class })
@TestPropertySource(locations = "classpath:binder-config-autoconfig.properties")
public class KafkaBinderAutoConfigurationPropertiesTest {

	@Autowired
	private KafkaMessageChannelBinder kafkaMessageChannelBinder;

	@Autowired
	private KafkaBinderHealthIndicator kafkaBinderHealthIndicator;

	@Test
	@SuppressWarnings("unchecked")
	public void testKafkaBinderConfigurationWithKafkaProperties() throws Exception {
		assertNotNull(this.kafkaMessageChannelBinder);
		ExtendedProducerProperties<KafkaProducerProperties> producerProperties = new ExtendedProducerProperties<>(
				new KafkaProducerProperties());
		Method getProducerFactoryMethod = KafkaMessageChannelBinder.class.getDeclaredMethod("getProducerFactory",
				String.class, ExtendedProducerProperties.class);
		getProducerFactoryMethod.setAccessible(true);
		DefaultKafkaProducerFactory producerFactory = (DefaultKafkaProducerFactory) getProducerFactoryMethod
				.invoke(this.kafkaMessageChannelBinder, "foo", producerProperties);
		Field producerFactoryConfigField = ReflectionUtils.findField(DefaultKafkaProducerFactory.class, "configs",
				Map.class);
		ReflectionUtils.makeAccessible(producerFactoryConfigField);
		Map<String, Object> producerConfigs = (Map<String, Object>) ReflectionUtils.getField(producerFactoryConfigField,
				producerFactory);
		assertTrue(producerConfigs.get("batch.size").equals(10));
		assertEquals(producerConfigs.get("key.serializer"), LongSerializer.class);
		assertNull(producerConfigs.get("key.deserializer"));
		assertEquals(producerConfigs.get("value.serializer"), LongSerializer.class);
		assertNull(producerConfigs.get("value.deserializer"));
		assertEquals("snappy", producerConfigs.get("compression.type"));
		List<String> bootstrapServers = new ArrayList<>();
		bootstrapServers.add("10.98.09.199:9092");
		bootstrapServers.add("10.98.09.196:9092");
		assertTrue((((List<String>) producerConfigs.get("bootstrap.servers")).containsAll(bootstrapServers)));
		Method createKafkaConsumerFactoryMethod = KafkaMessageChannelBinder.class.getDeclaredMethod(
				"createKafkaConsumerFactory", boolean.class, String.class, ExtendedConsumerProperties.class);
		createKafkaConsumerFactoryMethod.setAccessible(true);
		ExtendedConsumerProperties<KafkaConsumerProperties> consumerProperties = new ExtendedConsumerProperties<>(
				new KafkaConsumerProperties());
		DefaultKafkaConsumerFactory consumerFactory = (DefaultKafkaConsumerFactory) createKafkaConsumerFactoryMethod
				.invoke(this.kafkaMessageChannelBinder, true, "test", consumerProperties);
		Field consumerFactoryConfigField = ReflectionUtils.findField(DefaultKafkaConsumerFactory.class, "configs",
				Map.class);
		ReflectionUtils.makeAccessible(consumerFactoryConfigField);
		Map<String, Object> consumerConfigs = (Map<String, Object>) ReflectionUtils.getField(consumerFactoryConfigField,
				consumerFactory);
		assertEquals(consumerConfigs.get("key.deserializer"), LongDeserializer.class);
		assertNull(consumerConfigs.get("key.serializer"));
		assertEquals(consumerConfigs.get("value.deserializer"), LongDeserializer.class);
		assertNull(consumerConfigs.get("value.serialized"));
		assertEquals("groupIdFromBootConfig", consumerConfigs.get("group.id"));
		assertEquals("earliest", consumerConfigs.get("auto.offset.reset"));
		assertTrue((((List<String>) consumerConfigs.get("bootstrap.servers")).containsAll(bootstrapServers)));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testKafkaHealthIndicatorProperties() {
		assertNotNull(this.kafkaBinderHealthIndicator);
		Field consumerFactoryField = ReflectionUtils.findField(KafkaBinderHealthIndicator.class, "consumerFactory",
				ConsumerFactory.class);
		ReflectionUtils.makeAccessible(consumerFactoryField);
		DefaultKafkaConsumerFactory consumerFactory = (DefaultKafkaConsumerFactory) ReflectionUtils.getField(
				consumerFactoryField, this.kafkaBinderHealthIndicator);
		Field configField = ReflectionUtils.findField(DefaultKafkaConsumerFactory.class, "configs", Map.class);
		ReflectionUtils.makeAccessible(configField);
		Map<String, Object> configs = (Map<String, Object>) ReflectionUtils.getField(configField, consumerFactory);
		assertTrue(configs.containsKey("bootstrap.servers"));
		List<String> bootstrapServers = new ArrayList<>();
		bootstrapServers.add("10.98.09.199:9092");
		bootstrapServers.add("10.98.09.196:9092");
		assertTrue(((List<String>) configs.get("bootstrap.servers")).containsAll(bootstrapServers));
	}
}
