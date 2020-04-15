/*
 * Copyright 2017-2018 the original author or authors.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Barry Commins
 * @author Gary Russell
 * @author Laur Aliste
 * @author Soby Chacko
 */
public class KafkaBinderHealthIndicatorTest {

	private static final String TEST_TOPIC = "test";

	private static final String REGEX_TOPIC = "regex*";

	private KafkaBinderHealthIndicator indicator;

	@Mock
	private DefaultKafkaConsumerFactory consumerFactory;

	@Mock
	private KafkaConsumer consumer;

	@Mock
	private KafkaMessageChannelBinder binder;

	private final Map<String, KafkaMessageChannelBinder.TopicInformation> topicsInUse = new HashMap<>();

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		org.mockito.BDDMockito.given(consumerFactory.createConsumer()).willReturn(consumer);
		org.mockito.BDDMockito.given(binder.getTopicsInUse()).willReturn(topicsInUse);
		this.indicator = new KafkaBinderHealthIndicator(binder, consumerFactory);
		this.indicator.setTimeout(10);
	}

	@Test
	public void kafkaBinderIsUp() {
		final List<PartitionInfo> partitions = partitions(new Node(0, null, 0));
		topicsInUse.put(TEST_TOPIC, new KafkaMessageChannelBinder.TopicInformation("group1-healthIndicator", partitions, false));
		org.mockito.BDDMockito.given(consumer.partitionsFor(TEST_TOPIC)).willReturn(partitions);
		Health health = indicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
	}

	@Test
	public void kafkaBinderIsUpWithRegexTopic() {
		topicsInUse.put(REGEX_TOPIC, new KafkaMessageChannelBinder.TopicInformation("regex-healthIndicator", null, true));
		Health health = indicator.health();
		//verify no consumer interaction for retrieving partitions
		org.mockito.BDDMockito.verify(consumer, Mockito.never()).partitionsFor(REGEX_TOPIC);
		//Ensuring the normal health check returns with status "up"
		assertThat(health.getStatus()).isEqualTo(Status.UP);
	}

	@Test
	public void kafkaBinderIsDown() {
		final List<PartitionInfo> partitions = partitions(new Node(-1, null, 0));
		topicsInUse.put(TEST_TOPIC, new KafkaMessageChannelBinder.TopicInformation("group2-healthIndicator", partitions, false));
		org.mockito.BDDMockito.given(consumer.partitionsFor(TEST_TOPIC)).willReturn(partitions);
		Health health = indicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
	}

	@Test(timeout = 5000)
	public void kafkaBinderDoesNotAnswer() {
		final List<PartitionInfo> partitions = partitions(new Node(-1, null, 0));
		topicsInUse.put(TEST_TOPIC, new KafkaMessageChannelBinder.TopicInformation("group3-healthIndicator", partitions, false));
		org.mockito.BDDMockito.given(consumer.partitionsFor(TEST_TOPIC)).willAnswer(invocation -> {
			final int fiveMinutes = 1000 * 60 * 5;
			Thread.sleep(fiveMinutes);
			return partitions;
		});
		this.indicator.setTimeout(1);
		Health health = indicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
	}

	@Test
	public void createsConsumerOnceWhenInvokedMultipleTimes() {
		final List<PartitionInfo> partitions = partitions(new Node(0, null, 0));
		topicsInUse.put(TEST_TOPIC, new KafkaMessageChannelBinder.TopicInformation("group4-healthIndicator", partitions, false));
		org.mockito.BDDMockito.given(consumer.partitionsFor(TEST_TOPIC)).willReturn(partitions);

		indicator.health();
		Health health = indicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.UP);
		org.mockito.Mockito.verify(this.consumerFactory).createConsumer();
	}

	@Test
	public void consumerCreationFailsFirstTime() {
		final List<PartitionInfo> partitions = partitions(new Node(0, null, 0));
		topicsInUse.put(TEST_TOPIC, new KafkaMessageChannelBinder.TopicInformation("foo-healthIndicator", partitions, false));

		org.mockito.BDDMockito.given(consumerFactory.createConsumer()).willThrow(KafkaException.class)
				.willReturn(consumer);

		Health health = indicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);

		health = indicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);

		org.mockito.Mockito.verify(this.consumerFactory, Mockito.times(2)).createConsumer();
	}

	@Test
	public void testIfNoTopicsRegisteredByTheBinderProvidesDownStatus() {
		Health health = indicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
	}

	private List<PartitionInfo> partitions(Node leader) {
		List<PartitionInfo> partitions = new ArrayList<>();
		partitions.add(new PartitionInfo(TEST_TOPIC, 0, leader, null, null));
		return partitions;
	}
}
