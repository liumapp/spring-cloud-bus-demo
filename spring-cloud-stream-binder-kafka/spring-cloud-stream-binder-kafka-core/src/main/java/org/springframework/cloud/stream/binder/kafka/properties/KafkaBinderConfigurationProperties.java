/*
 * Copyright 2015-2018 the original author or authors.
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

package org.springframework.cloud.stream.binder.kafka.properties;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
import org.springframework.cloud.stream.binder.HeaderMode;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaProducerProperties.CompressionType;
import org.springframework.expression.Expression;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for the Kafka binder.
 * The properties in this class are prefixed with <b>spring.cloud.stream.kafka.binder</b>.
 *
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 * @author Marius Bogoevici
 * @author Soby Chacko
 * @author Gary Russell
 * @author Rafal Zukowski
 * @author Aldo Sinanaj
 */
@ConfigurationProperties(prefix = "spring.cloud.stream.kafka.binder")
public class KafkaBinderConfigurationProperties {

	private static final String DEFAULT_KAFKA_CONNECTION_STRING = "localhost:9092";

	private final Transaction transaction = new Transaction();

	private final KafkaProperties kafkaProperties;

	private String[] zkNodes = new String[] { "localhost" };

	/**
	 * Arbitrary kafka properties that apply to both producers and consumers.
	 */
	private Map<String, String> configuration = new HashMap<>();

	/**
	 * Arbitrary kafka consumer properties.
	 */
	private Map<String, String> consumerProperties = new HashMap<>();

	/**
	 * Arbitrary kafka producer properties.
	 */
	private Map<String, String> producerProperties = new HashMap<>();

	private String defaultZkPort = "2181";

	private String[] brokers = new String[] { "localhost" };

	private String defaultBrokerPort = "9092";

	private String[] headers = new String[] {};

	private int offsetUpdateTimeWindow = 10000;

	private int offsetUpdateCount;

	private int offsetUpdateShutdownTimeout = 2000;

	private int maxWait = 100;

	private boolean autoCreateTopics = true;

	private boolean autoAddPartitions;

	private int socketBufferSize = 2097152;

	/**
	 * ZK session timeout in milliseconds.
	 */
	private int zkSessionTimeout = 10000;

	/**
	 * ZK Connection timeout in milliseconds.
	 */
	private int zkConnectionTimeout = 10000;

	private String requiredAcks = "1";

	private short replicationFactor = 1;

	private int fetchSize = 1024 * 1024;

	private int minPartitionCount = 1;

	private int queueSize = 8192;

	/**
	 * Time to wait to get partition information in seconds; default 60.
	 */
	private int healthTimeout = 60;

	private JaasLoginModuleConfiguration jaas;

	/**
	 * The bean name of a custom header mapper to use instead of a {@link org.springframework.kafka.support.DefaultKafkaHeaderMapper}.
	 */
	private String headerMapperBeanName;


	public KafkaBinderConfigurationProperties(KafkaProperties kafkaProperties) {
		Assert.notNull(kafkaProperties, "'kafkaProperties' cannot be null");
		this.kafkaProperties = kafkaProperties;
	}

	public KafkaProperties getKafkaProperties() {
		return this.kafkaProperties;
	}

	public Transaction getTransaction() {
		return this.transaction;
	}

	/**
	 * No longer used.
	 * @return the connection String
	 * @deprecated connection to zookeeper is no longer necessary
	 */
	@DeprecatedConfigurationProperty(reason = "Not used since 2.0")
	@Deprecated
	public String getZkConnectionString() {
		return toConnectionString(this.zkNodes, this.defaultZkPort);
	}

	public String getKafkaConnectionString() {
		return toConnectionString(this.brokers, this.defaultBrokerPort);
	}

	public String getDefaultKafkaConnectionString() {
		return DEFAULT_KAFKA_CONNECTION_STRING;
	}

	public String[] getHeaders() {
		return this.headers;
	}

	/**
	 * No longer used.
	 * @return the window.
	 * @deprecated No longer used by the binder
	 */
	@Deprecated
	@DeprecatedConfigurationProperty(reason = "Not used since 2.0")
	public int getOffsetUpdateTimeWindow() {
		return this.offsetUpdateTimeWindow;
	}

	/**
	 * No longer used.
	 * @return the count.
	 * @deprecated No longer used by the binder
	 */
	@Deprecated
	@DeprecatedConfigurationProperty(reason = "Not used since 2.0")
	public int getOffsetUpdateCount() {
		return this.offsetUpdateCount;
	}

	/**
	 * No longer used.
	 * @return the timeout.
	 * @deprecated No longer used by the binder
	 */
	@Deprecated
	@DeprecatedConfigurationProperty(reason = "Not used since 2.0")
	public int getOffsetUpdateShutdownTimeout() {
		return this.offsetUpdateShutdownTimeout;
	}

	/**
	 * Zookeeper nodes.
	 * @return the nodes.
	 * @deprecated connection to zookeeper is no longer necessary
	 */
	@Deprecated
	@DeprecatedConfigurationProperty(reason = "No longer necessary since 2.0")
	public String[] getZkNodes() {
		return this.zkNodes;
	}

	/**
	 * Zookeeper nodes.
	 * @param zkNodes the nodes.
	 * @deprecated connection to zookeeper is no longer necessary
	 */
	@Deprecated
	@DeprecatedConfigurationProperty(reason = "No longer necessary since 2.0")
	public void setZkNodes(String... zkNodes) {
		this.zkNodes = zkNodes;
	}

	/**
	 * Zookeeper port.
	 * @param defaultZkPort the port.
	 * @deprecated connection to zookeeper is no longer necessary
	 */
	@Deprecated
	@DeprecatedConfigurationProperty(reason = "No longer necessary since 2.0")
	public void setDefaultZkPort(String defaultZkPort) {
		this.defaultZkPort = defaultZkPort;
	}

	public String[] getBrokers() {
		return this.brokers;
	}

	public void setBrokers(String... brokers) {
		this.brokers = brokers;
	}

	public void setDefaultBrokerPort(String defaultBrokerPort) {
		this.defaultBrokerPort = defaultBrokerPort;
	}

	public void setHeaders(String... headers) {
		this.headers = headers;
	}

	/**
	 * No longer used.
	 * @param offsetUpdateTimeWindow the window.
	 * @deprecated No longer used by the binder
	 */
	@Deprecated
	@DeprecatedConfigurationProperty(reason = "Not used since 2.0")
	public void setOffsetUpdateTimeWindow(int offsetUpdateTimeWindow) {
		this.offsetUpdateTimeWindow = offsetUpdateTimeWindow;
	}

	/**
	 * No longer used.
	 * @param offsetUpdateCount the count.
	 * @deprecated No longer used by the binder
	 */
	@Deprecated
	@DeprecatedConfigurationProperty(reason = "Not used since 2.0")
	public void setOffsetUpdateCount(int offsetUpdateCount) {
		this.offsetUpdateCount = offsetUpdateCount;
	}

	/**
	 * No longer used.
	 * @param offsetUpdateShutdownTimeout the timeout.
	 * @deprecated No longer used by the binder
	 */
	@Deprecated
	@DeprecatedConfigurationProperty(reason = "Not used since 2.0")
	public void setOffsetUpdateShutdownTimeout(int offsetUpdateShutdownTimeout) {
		this.offsetUpdateShutdownTimeout = offsetUpdateShutdownTimeout;
	}

	/**
	 * Zookeeper session timeout.
	 * @return the timeout.
	 * @deprecated connection to zookeeper is no longer necessary
	 */
	@Deprecated
	@DeprecatedConfigurationProperty(reason = "No longer necessary since 2.0")
	public int getZkSessionTimeout() {
		return this.zkSessionTimeout;
	}

	/**
	 * Zookeeper session timeout.
	 * @param zkSessionTimeout the timout
	 * @deprecated connection to zookeeper is no longer necessary
	 */
	@Deprecated
	@DeprecatedConfigurationProperty(reason = "No longer necessary since 2.0")
	public void setZkSessionTimeout(int zkSessionTimeout) {
		this.zkSessionTimeout = zkSessionTimeout;
	}

	/**
	 * Zookeeper connection timeout.
	 * @return the timout.
	 * @deprecated connection to zookeeper is no longer necessary
	 */
	@Deprecated
	@DeprecatedConfigurationProperty(reason = "No longer necessary since 2.0")
	public int getZkConnectionTimeout() {
		return this.zkConnectionTimeout;
	}

	/**
	 * Zookeeper connection timeout.
	 * @param zkConnectionTimeout the timeout.
	 * @deprecated connection to zookeeper is no longer necessary
	 */
	@Deprecated
	@DeprecatedConfigurationProperty(reason = "No longer necessary since 2.0")
	public void setZkConnectionTimeout(int zkConnectionTimeout) {
		this.zkConnectionTimeout = zkConnectionTimeout;
	}

	/**
	 * Converts an array of host values to a comma-separated String.
	 * It will append the default port value, if not already specified.
	 *
	 * @param hosts host string
	 * @param defaultPort port
	 * @return formatted connection string
	 */
	private String toConnectionString(String[] hosts, String defaultPort) {
		String[] fullyFormattedHosts = new String[hosts.length];
		for (int i = 0; i < hosts.length; i++) {
			if (hosts[i].contains(":") || StringUtils.isEmpty(defaultPort)) {
				fullyFormattedHosts[i] = hosts[i];
			}
			else {
				fullyFormattedHosts[i] = hosts[i] + ":" + defaultPort;
			}
		}
		return StringUtils.arrayToCommaDelimitedString(fullyFormattedHosts);
	}

	/**
	 * No longer used.
	 * @return the wait.
	 * @deprecated No longer used by the binder
	 */
	@Deprecated
	@DeprecatedConfigurationProperty(reason = "Not used since 2.0")
	public int getMaxWait() {
		return this.maxWait;
	}

	/**
	 * No longer user.
	 * @param maxWait the wait.
	 * @deprecated No longer used by the binder
	 */
	@Deprecated
	@DeprecatedConfigurationProperty(reason = "Not used since 2.0")
	public void setMaxWait(int maxWait) {
		this.maxWait = maxWait;
	}

	public String getRequiredAcks() {
		return this.requiredAcks;
	}

	public void setRequiredAcks(String requiredAcks) {
		this.requiredAcks = requiredAcks;
	}

	public short getReplicationFactor() {
		return this.replicationFactor;
	}

	public void setReplicationFactor(short replicationFactor) {
		this.replicationFactor = replicationFactor;
	}

	/**
	 * No longer used.
	 * @return the size.
	 * @deprecated No longer used by the binder
	 */
	@Deprecated
	@DeprecatedConfigurationProperty(reason = "Not used since 2.0")
	public int getFetchSize() {
		return this.fetchSize;
	}

	/**
	 * No longer used.
	 * @param fetchSize the size.
	 * @deprecated No longer used by the binder
	 */
	@Deprecated
	@DeprecatedConfigurationProperty(reason = "Not used since 2.0")
	public void setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
	}

	public int getMinPartitionCount() {
		return this.minPartitionCount;
	}

	public void setMinPartitionCount(int minPartitionCount) {
		this.minPartitionCount = minPartitionCount;
	}

	public int getHealthTimeout() {
		return this.healthTimeout;
	}

	public void setHealthTimeout(int healthTimeout) {
		this.healthTimeout = healthTimeout;
	}

	/**
	 * No longer used.
	 * @return the queue size.
	 * @deprecated No longer used by the binder
	 */
	@Deprecated
	@DeprecatedConfigurationProperty(reason = "Not used since 2.0")
	public int getQueueSize() {
		return this.queueSize;
	}

	/**
	 * No longer used.
	 * @param queueSize the queue size.
	 * @deprecated No longer used by the binder
	 */
	@Deprecated
	@DeprecatedConfigurationProperty(reason = "Not used since 2.0")
	public void setQueueSize(int queueSize) {
		this.queueSize = queueSize;
	}

	public boolean isAutoCreateTopics() {
		return this.autoCreateTopics;
	}

	public void setAutoCreateTopics(boolean autoCreateTopics) {
		this.autoCreateTopics = autoCreateTopics;
	}

	public boolean isAutoAddPartitions() {
		return this.autoAddPartitions;
	}

	public void setAutoAddPartitions(boolean autoAddPartitions) {
		this.autoAddPartitions = autoAddPartitions;
	}

	/**
	 * No longer used; set properties such as this via {@link #getConfiguration()
	 * configuration}.
	 * @return the size.
	 * @deprecated No longer used by the binder
	 */
	@Deprecated
	@DeprecatedConfigurationProperty(reason = "Not used since 2.0, set properties such as this via 'configuration'")
	public int getSocketBufferSize() {
		return this.socketBufferSize;
	}

	/**
	 * No longer used; set properties such as this via {@link #getConfiguration()
	 * configuration}.
	 * @param socketBufferSize the size.
	 * @deprecated No longer used by the binder
	 */
	@Deprecated
	@DeprecatedConfigurationProperty(reason = "Not used since 2.0, set properties such as this via 'configuration'")
	public void setSocketBufferSize(int socketBufferSize) {
		this.socketBufferSize = socketBufferSize;
	}

	public Map<String, String> getConfiguration() {
		return this.configuration;
	}

	public void setConfiguration(Map<String, String> configuration) {
		this.configuration = configuration;
	}

	public Map<String, String> getConsumerProperties() {
		return this.consumerProperties;
	}

	public void setConsumerProperties(Map<String, String> consumerProperties) {
		Assert.notNull(consumerProperties, "'consumerProperties' cannot be null");
		this.consumerProperties = consumerProperties;
	}

	public Map<String, String> getProducerProperties() {
		return this.producerProperties;
	}

	public void setProducerProperties(Map<String, String> producerProperties) {
		Assert.notNull(producerProperties, "'producerProperties' cannot be null");
		this.producerProperties = producerProperties;
	}

	/**
	 * Merge boot consumer properties, general properties from
	 * {@link #setConfiguration(Map)} that apply to consumers, properties from
	 * {@link #setConsumerProperties(Map)}, in that order.
	 * @return the merged properties.
	 */
	public Map<String, Object> mergedConsumerConfiguration() {
		Map<String, Object> consumerConfiguration = new HashMap<>();
		consumerConfiguration.putAll(this.kafkaProperties.buildConsumerProperties());
		// Copy configured binder properties that apply to consumers
		for (Map.Entry<String, String> configurationEntry : this.configuration.entrySet()) {
			if (ConsumerConfig.configNames().contains(configurationEntry.getKey())) {
				consumerConfiguration.put(configurationEntry.getKey(), configurationEntry.getValue());
			}
		}
		consumerConfiguration.putAll(this.consumerProperties);
		// Override Spring Boot bootstrap server setting if left to default with the value
		// configured in the binder
		return getConfigurationWithBootstrapServer(consumerConfiguration, ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG);
	}

	/**
	 * Merge boot producer properties, general properties from
	 * {@link #setConfiguration(Map)} that apply to producers, properties from
	 * {@link #setProducerProperties(Map)}, in that order.
	 * @return the merged properties.
	 */
	public Map<String, Object> mergedProducerConfiguration() {
		Map<String, Object> producerConfiguration = new HashMap<>();
		producerConfiguration.putAll(this.kafkaProperties.buildProducerProperties());
		// Copy configured binder properties that apply to producers
		for (Map.Entry<String, String> configurationEntry : this.configuration.entrySet()) {
			if (ProducerConfig.configNames().contains(configurationEntry.getKey())) {
				producerConfiguration.put(configurationEntry.getKey(), configurationEntry.getValue());
			}
		}
		producerConfiguration.putAll(this.producerProperties);
		// Override Spring Boot bootstrap server setting if left to default with the value
		// configured in the binder
		return getConfigurationWithBootstrapServer(producerConfiguration, ProducerConfig.BOOTSTRAP_SERVERS_CONFIG);
	}

	private Map<String, Object> getConfigurationWithBootstrapServer(Map<String, Object> configuration, String bootstrapServersConfig) {
		if (ObjectUtils.isEmpty(configuration.get(bootstrapServersConfig))) {
			configuration.put(bootstrapServersConfig, getKafkaConnectionString());
		}
		else {
			Object boostrapServersConfig = configuration.get(bootstrapServersConfig);
			if (boostrapServersConfig instanceof List) {
				@SuppressWarnings("unchecked")
				List<String> bootStrapServers = (List<String>) configuration
						.get(bootstrapServersConfig);
				if (bootStrapServers.size() == 1 && bootStrapServers.get(0).equals("localhost:9092")) {
					configuration.put(bootstrapServersConfig, getKafkaConnectionString());
				}
			}
		}
		return Collections.unmodifiableMap(configuration);
	}

	public JaasLoginModuleConfiguration getJaas() {
		return this.jaas;
	}

	public void setJaas(JaasLoginModuleConfiguration jaas) {
		this.jaas = jaas;
	}

	public String getHeaderMapperBeanName() {
		return this.headerMapperBeanName;
	}

	public void setHeaderMapperBeanName(String headerMapperBeanName) {
		this.headerMapperBeanName = headerMapperBeanName;
	}

	/**
	 * Domain class that models transaction capabilities in Kafka.
	 */
	public static class Transaction {

		private final CombinedProducerProperties producer = new CombinedProducerProperties();

		private String transactionIdPrefix;

		public String getTransactionIdPrefix() {
			return this.transactionIdPrefix;
		}

		public void setTransactionIdPrefix(String transactionIdPrefix) {
			this.transactionIdPrefix = transactionIdPrefix;
		}

		public CombinedProducerProperties getProducer() {
			return this.producer;
		}

	}

	/**
	 * An combination of {@link ProducerProperties} and {@link KafkaProducerProperties}
	 * so that common and kafka-specific properties can be set for the transactional
	 * producer.
	 * @since 2.1
	 */
	public static class CombinedProducerProperties {

		private final ProducerProperties producerProperties = new ProducerProperties();

		private final KafkaProducerProperties kafkaProducerProperties = new KafkaProducerProperties();

		public Expression getPartitionKeyExpression() {
			return this.producerProperties.getPartitionKeyExpression();
		}

		public void setPartitionKeyExpression(Expression partitionKeyExpression) {
			this.producerProperties.setPartitionKeyExpression(partitionKeyExpression);
		}

		public boolean isPartitioned() {
			return this.producerProperties.isPartitioned();
		}

		public Expression getPartitionSelectorExpression() {
			return this.producerProperties.getPartitionSelectorExpression();
		}

		public void setPartitionSelectorExpression(Expression partitionSelectorExpression) {
			this.producerProperties.setPartitionSelectorExpression(partitionSelectorExpression);
		}

		public @Min(value = 1, message = "Partition count should be greater than zero.") int getPartitionCount() {
			return this.producerProperties.getPartitionCount();
		}

		public void setPartitionCount(int partitionCount) {
			this.producerProperties.setPartitionCount(partitionCount);
		}

		public String[] getRequiredGroups() {
			return this.producerProperties.getRequiredGroups();
		}

		public void setRequiredGroups(String... requiredGroups) {
			this.producerProperties.setRequiredGroups(requiredGroups);
		}

		public @AssertTrue(message = "Partition key expression and partition key extractor class properties are mutually exclusive.") boolean isValidPartitionKeyProperty() {
			return this.producerProperties.isValidPartitionKeyProperty();
		}

		public @AssertTrue(message = "Partition selector class and partition selector expression properties are mutually exclusive.") boolean isValidPartitionSelectorProperty() {
			return this.producerProperties.isValidPartitionSelectorProperty();
		}

		public HeaderMode getHeaderMode() {
			return this.producerProperties.getHeaderMode();
		}

		public void setHeaderMode(HeaderMode headerMode) {
			this.producerProperties.setHeaderMode(headerMode);
		}

		public boolean isUseNativeEncoding() {
			return this.producerProperties.isUseNativeEncoding();
		}

		public void setUseNativeEncoding(boolean useNativeEncoding) {
			this.producerProperties.setUseNativeEncoding(useNativeEncoding);
		}

		public boolean isErrorChannelEnabled() {
			return this.producerProperties.isErrorChannelEnabled();
		}

		public void setErrorChannelEnabled(boolean errorChannelEnabled) {
			this.producerProperties.setErrorChannelEnabled(errorChannelEnabled);
		}

		public String getPartitionKeyExtractorName() {
			return this.producerProperties.getPartitionKeyExtractorName();
		}

		public void setPartitionKeyExtractorName(String partitionKeyExtractorName) {
			this.producerProperties.setPartitionKeyExtractorName(partitionKeyExtractorName);
		}

		public String getPartitionSelectorName() {
			return this.producerProperties.getPartitionSelectorName();
		}

		public void setPartitionSelectorName(String partitionSelectorName) {
			this.producerProperties.setPartitionSelectorName(partitionSelectorName);
		}

		public int getBufferSize() {
			return this.kafkaProducerProperties.getBufferSize();
		}

		public void setBufferSize(int bufferSize) {
			this.kafkaProducerProperties.setBufferSize(bufferSize);
		}

		public @NotNull CompressionType getCompressionType() {
			return this.kafkaProducerProperties.getCompressionType();
		}

		public void setCompressionType(CompressionType compressionType) {
			this.kafkaProducerProperties.setCompressionType(compressionType);
		}

		public boolean isSync() {
			return this.kafkaProducerProperties.isSync();
		}

		public void setSync(boolean sync) {
			this.kafkaProducerProperties.setSync(sync);
		}

		public int getBatchTimeout() {
			return this.kafkaProducerProperties.getBatchTimeout();
		}

		public void setBatchTimeout(int batchTimeout) {
			this.kafkaProducerProperties.setBatchTimeout(batchTimeout);
		}

		public Expression getMessageKeyExpression() {
			return this.kafkaProducerProperties.getMessageKeyExpression();
		}

		public void setMessageKeyExpression(Expression messageKeyExpression) {
			this.kafkaProducerProperties.setMessageKeyExpression(messageKeyExpression);
		}

		public String[] getHeaderPatterns() {
			return this.kafkaProducerProperties.getHeaderPatterns();
		}

		public void setHeaderPatterns(String[] headerPatterns) {
			this.kafkaProducerProperties.setHeaderPatterns(headerPatterns);
		}

		public Map<String, String> getConfiguration() {
			return this.kafkaProducerProperties.getConfiguration();
		}

		public void setConfiguration(Map<String, String> configuration) {
			this.kafkaProducerProperties.setConfiguration(configuration);
		}

		@SuppressWarnings("deprecation")
		public KafkaAdminProperties getAdmin() {
			return this.kafkaProducerProperties.getAdmin();
		}

		@SuppressWarnings("deprecation")
		public void setAdmin(KafkaAdminProperties admin) {
			this.kafkaProducerProperties.setAdmin(admin);
		}

		public KafkaTopicProperties getTopic() {
			return this.kafkaProducerProperties.getTopic();
		}

		public void setTopic(KafkaTopicProperties topic) {
			this.kafkaProducerProperties.setTopic(topic);
		}

		public KafkaProducerProperties getExtension() {
			return this.kafkaProducerProperties;
		}
	}

}
