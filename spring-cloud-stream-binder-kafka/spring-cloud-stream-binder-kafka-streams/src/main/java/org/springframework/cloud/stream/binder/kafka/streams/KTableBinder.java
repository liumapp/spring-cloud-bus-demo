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

import org.apache.kafka.streams.kstream.KTable;

import org.springframework.cloud.stream.binder.AbstractBinder;
import org.springframework.cloud.stream.binder.BinderSpecificPropertiesProvider;
import org.springframework.cloud.stream.binder.Binding;
import org.springframework.cloud.stream.binder.DefaultBinding;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.ExtendedPropertiesBinder;
import org.springframework.cloud.stream.binder.kafka.provisioning.KafkaTopicProvisioner;
import org.springframework.cloud.stream.binder.kafka.streams.properties.KafkaStreamsBinderConfigurationProperties;
import org.springframework.cloud.stream.binder.kafka.streams.properties.KafkaStreamsConsumerProperties;
import org.springframework.cloud.stream.binder.kafka.streams.properties.KafkaStreamsExtendedBindingProperties;
import org.springframework.cloud.stream.binder.kafka.streams.properties.KafkaStreamsProducerProperties;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.cloud.stream.binder.Binder} implementation for {@link KTable}.
 * This implemenation extends from the {@link AbstractBinder} directly.
 *
 * Provides only consumer binding for the bound KTable as output bindings are not allowed on it.
 *
 * @author Soby Chacko
 */
class KTableBinder extends
		AbstractBinder<KTable<Object, Object>, ExtendedConsumerProperties<KafkaStreamsConsumerProperties>, ExtendedProducerProperties<KafkaStreamsProducerProperties>>
		implements ExtendedPropertiesBinder<KTable<Object, Object>, KafkaStreamsConsumerProperties, KafkaStreamsProducerProperties> {

	private final KafkaStreamsBinderConfigurationProperties binderConfigurationProperties;

	private final KafkaTopicProvisioner kafkaTopicProvisioner;

	private Map<String, KafkaStreamsDlqDispatch> kafkaStreamsDlqDispatchers;

	private KafkaStreamsExtendedBindingProperties kafkaStreamsExtendedBindingProperties = new KafkaStreamsExtendedBindingProperties();

	KTableBinder(KafkaStreamsBinderConfigurationProperties binderConfigurationProperties, KafkaTopicProvisioner kafkaTopicProvisioner,
				Map<String, KafkaStreamsDlqDispatch> kafkaStreamsDlqDispatchers) {
		this.binderConfigurationProperties = binderConfigurationProperties;
		this.kafkaTopicProvisioner = kafkaTopicProvisioner;
		this.kafkaStreamsDlqDispatchers = kafkaStreamsDlqDispatchers;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Binding<KTable<Object, Object>> doBindConsumer(String name, String group, KTable<Object, Object> inputTarget,
															ExtendedConsumerProperties<KafkaStreamsConsumerProperties> properties) {
		if (!StringUtils.hasText(group)) {
			group = this.binderConfigurationProperties.getApplicationId();
		}
		KafkaStreamsBinderUtils.prepareConsumerBinding(name, group, getApplicationContext(),
				this.kafkaTopicProvisioner,
				this.binderConfigurationProperties, properties, this.kafkaStreamsDlqDispatchers);
		return new DefaultBinding<>(name, group, inputTarget, null);
	}

	@Override
	protected Binding<KTable<Object, Object>> doBindProducer(String name, KTable<Object, Object> outboundBindTarget,
															ExtendedProducerProperties<KafkaStreamsProducerProperties> properties) {
		throw new UnsupportedOperationException("No producer level binding is allowed for KTable");
	}

	@Override
	public KafkaStreamsConsumerProperties getExtendedConsumerProperties(String channelName) {
		return this.kafkaStreamsExtendedBindingProperties.getExtendedConsumerProperties(channelName);
	}

	@Override
	public KafkaStreamsProducerProperties getExtendedProducerProperties(String channelName) {
		return this.kafkaStreamsExtendedBindingProperties.getExtendedProducerProperties(channelName);
	}

	@Override
	public String getDefaultsPrefix() {
		return this.kafkaStreamsExtendedBindingProperties.getDefaultsPrefix();
	}

	@Override
	public Class<? extends BinderSpecificPropertiesProvider> getExtendedPropertiesEntryClass() {
		return this.kafkaStreamsExtendedBindingProperties.getExtendedPropertiesEntryClass();
	}

	public void setKafkaStreamsExtendedBindingProperties(
			KafkaStreamsExtendedBindingProperties kafkaStreamsExtendedBindingProperties) {
		this.kafkaStreamsExtendedBindingProperties = kafkaStreamsExtendedBindingProperties;
	}
}
