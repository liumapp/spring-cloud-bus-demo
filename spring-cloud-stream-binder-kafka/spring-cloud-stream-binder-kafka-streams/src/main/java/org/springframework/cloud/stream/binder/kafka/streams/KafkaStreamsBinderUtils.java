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

import java.util.Map;

import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaConsumerProperties;
import org.springframework.cloud.stream.binder.kafka.provisioning.KafkaTopicProvisioner;
import org.springframework.cloud.stream.binder.kafka.streams.properties.KafkaStreamsBinderConfigurationProperties;
import org.springframework.cloud.stream.binder.kafka.streams.properties.KafkaStreamsConsumerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

/**
 * Common methods used by various Kafka Streams types across the binders.
 *
 * @author Soby Chacko
 */
final class KafkaStreamsBinderUtils {

	private KafkaStreamsBinderUtils() {

	}

	static void prepareConsumerBinding(String name, String group, ApplicationContext context,
											KafkaTopicProvisioner kafkaTopicProvisioner,
											KafkaStreamsBinderConfigurationProperties binderConfigurationProperties,
											ExtendedConsumerProperties<KafkaStreamsConsumerProperties> properties,
											Map<String, KafkaStreamsDlqDispatch> kafkaStreamsDlqDispatchers) {
		ExtendedConsumerProperties<KafkaConsumerProperties> extendedConsumerProperties = new ExtendedConsumerProperties<>(
				properties.getExtension());
		if (binderConfigurationProperties.getSerdeError() == KafkaStreamsBinderConfigurationProperties.SerdeError.sendToDlq) {
			extendedConsumerProperties.getExtension().setEnableDlq(true);
		}

		String[] inputTopics = StringUtils.commaDelimitedListToStringArray(name);
		for (String inputTopic : inputTopics) {
			kafkaTopicProvisioner.provisionConsumerDestination(inputTopic, group, extendedConsumerProperties);
		}

		if (extendedConsumerProperties.getExtension().isEnableDlq()) {
			KafkaStreamsDlqDispatch kafkaStreamsDlqDispatch = !StringUtils.isEmpty(extendedConsumerProperties.getExtension().getDlqName()) ?
					new KafkaStreamsDlqDispatch(extendedConsumerProperties.getExtension().getDlqName(), binderConfigurationProperties,
							extendedConsumerProperties.getExtension()) : null;
			for (String inputTopic : inputTopics) {
				if (StringUtils.isEmpty(extendedConsumerProperties.getExtension().getDlqName())) {
					String dlqName = "error." + inputTopic + "." + group;
					kafkaStreamsDlqDispatch = new KafkaStreamsDlqDispatch(dlqName, binderConfigurationProperties,
							extendedConsumerProperties.getExtension());
				}

				SendToDlqAndContinue sendToDlqAndContinue = context.getBean(SendToDlqAndContinue.class);
				sendToDlqAndContinue.addKStreamDlqDispatch(inputTopic, kafkaStreamsDlqDispatch);

				kafkaStreamsDlqDispatchers.put(inputTopic, kafkaStreamsDlqDispatch);
			}
		}
	}

}
