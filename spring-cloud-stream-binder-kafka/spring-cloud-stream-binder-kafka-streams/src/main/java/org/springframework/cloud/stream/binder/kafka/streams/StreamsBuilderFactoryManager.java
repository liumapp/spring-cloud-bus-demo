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

import java.util.Set;

import org.springframework.context.SmartLifecycle;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;

/**
 * Iterate through all {@link StreamsBuilderFactoryBean} in the application context
 * and start them. As each one completes starting, register the associated KafkaStreams
 * object into {@link QueryableStoreRegistry}.
 *
 * This {@link SmartLifecycle} class ensures that the bean created from it is started very late
 * through the bootstrap process by setting the phase value closer to Integer.MAX_VALUE.
 * This is to guarantee that the {@link StreamsBuilderFactoryBean} on a
 * {@link org.springframework.cloud.stream.annotation.StreamListener} method with multiple
 * bindings is only started after all the binding phases have completed successfully.
 *
 * @author Soby Chacko
 */
class StreamsBuilderFactoryManager implements SmartLifecycle {

	private final KafkaStreamsBindingInformationCatalogue kafkaStreamsBindingInformationCatalogue;
	private final KafkaStreamsRegistry kafkaStreamsRegistry;

	private volatile boolean running;

	StreamsBuilderFactoryManager(KafkaStreamsBindingInformationCatalogue kafkaStreamsBindingInformationCatalogue,
								KafkaStreamsRegistry kafkaStreamsRegistry) {
		this.kafkaStreamsBindingInformationCatalogue = kafkaStreamsBindingInformationCatalogue;
		this.kafkaStreamsRegistry = kafkaStreamsRegistry;
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public void stop(Runnable callback) {
		stop();
		if (callback != null) {
			callback.run();
		}
	}

	@Override
	public synchronized void start() {
		if (!this.running) {
			try {
				Set<StreamsBuilderFactoryBean> streamsBuilderFactoryBeans = this.kafkaStreamsBindingInformationCatalogue.getStreamsBuilderFactoryBeans();
				for (StreamsBuilderFactoryBean streamsBuilderFactoryBean : streamsBuilderFactoryBeans) {
					streamsBuilderFactoryBean.start();
					this.kafkaStreamsRegistry.registerKafkaStreams(streamsBuilderFactoryBean.getKafkaStreams());
				}
				this.running = true;
			}
			catch (Exception ex) {
				throw new KafkaException("Could not start stream: ", ex);
			}
		}
	}

	@Override
	public synchronized  void stop() {
			if (this.running) {
				try {
					Set<StreamsBuilderFactoryBean> streamsBuilderFactoryBeans = this.kafkaStreamsBindingInformationCatalogue.getStreamsBuilderFactoryBeans();
					for (StreamsBuilderFactoryBean streamsBuilderFactoryBean : streamsBuilderFactoryBeans) {
						streamsBuilderFactoryBean.stop();
					}
				}
				catch (Exception ex) {
					throw new IllegalStateException(ex);
				}
				finally {
					this.running = false;
				}
			}
	}

	@Override
	public synchronized boolean isRunning() {
		return this.running;
	}

	@Override
	public int getPhase() {
		return Integer.MAX_VALUE - 100;
	}

}
