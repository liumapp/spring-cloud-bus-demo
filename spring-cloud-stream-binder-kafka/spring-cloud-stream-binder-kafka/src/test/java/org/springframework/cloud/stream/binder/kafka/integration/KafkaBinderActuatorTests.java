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

package org.springframework.cloud.stream.binder.kafka.integration;

import java.util.List;
import java.util.Map;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.binder.Binding;
import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.cloud.stream.config.ListenerContainerCustomizer;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.test.rule.EmbeddedKafkaRule;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 * @author Oleg Zhurakousky
 * @author Jon Schneider
 *
 * @since 2.0
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = "spring.cloud.stream.bindings.input.group=" + KafkaBinderActuatorTests.TEST_CONSUMER_GROUP)
public class KafkaBinderActuatorTests {

	static final String TEST_CONSUMER_GROUP = "testGroup-actuatorTests";

	private static final String KAFKA_BROKERS_PROPERTY = "spring.kafka.bootstrap-servers";

	@ClassRule
	public static EmbeddedKafkaRule kafkaEmbedded = new EmbeddedKafkaRule(1, true);

	@BeforeClass
	public static void setup() {
		System.setProperty(KAFKA_BROKERS_PROPERTY, kafkaEmbedded.getEmbeddedKafka().getBrokersAsString());
	}

	@AfterClass
	public static void clean() {
		System.clearProperty(KAFKA_BROKERS_PROPERTY);
	}

	@Autowired
	private MeterRegistry meterRegistry;

	@Autowired
	private KafkaTemplate<?, byte[]> kafkaTemplate;

	@Test
	public void testKafkaBinderMetricsExposed() {
		this.kafkaTemplate.send(Sink.INPUT, null, "foo".getBytes());
		this.kafkaTemplate.flush();

		assertThat(this.meterRegistry.get("spring.cloud.stream.binder.kafka.offset")
				.tag("group", TEST_CONSUMER_GROUP)
				.tag("topic", Sink.INPUT)
				.gauge().value()).isGreaterThan(0);
	}

	@Test
	public void testKafkaBinderMetricsWhenNoMicrometer() {
		new ApplicationContextRunner()
				.withUserConfiguration(KafkaMetricsTestConfig.class)
				.withClassLoader(new FilteredClassLoader("io.micrometer.core"))
				.run(context -> {
					assertThat(context.getBeanNamesForType(MeterRegistry.class)).isEmpty();
					assertThat(context.getBeanNamesForType(MeterBinder.class)).isEmpty();

					DirectFieldAccessor channelBindingServiceAccessor = new DirectFieldAccessor(context.getBean(BindingService.class));
					@SuppressWarnings("unchecked")
					Map<String, List<Binding<MessageChannel>>> consumerBindings = (Map<String, List<Binding<MessageChannel>>>) channelBindingServiceAccessor
							.getPropertyValue("consumerBindings");
					assertThat(new DirectFieldAccessor(consumerBindings.get("input").get(0)).getPropertyValue("lifecycle.messageListenerContainer.beanName"))
						.isEqualTo("setByCustomizer:input");
				});
	}

	@EnableBinding(Sink.class)
	@EnableAutoConfiguration
	public static class KafkaMetricsTestConfig {

		@Bean
		public ListenerContainerCustomizer<AbstractMessageListenerContainer<?, ?>> containerCustomizer() {
			return (c, q, g) -> c.setBeanName("setByCustomizer:" + q);
		}

		@StreamListener(Sink.INPUT)
		public void process(String payload) throws InterruptedException {
			// Artificial slow listener to emulate consumer lag
			Thread.sleep(1000);
		}

	}

}
