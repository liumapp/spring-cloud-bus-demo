/*
 * Copyright 2014-2018 the original author or authors.
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

package org.springframework.cloud.stream.binder.kafka.provisioning;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.CreatePartitionsResult;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.cloud.stream.binder.BinderException;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaBinderConfigurationProperties;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaConsumerProperties;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaProducerProperties;
import org.springframework.cloud.stream.binder.kafka.properties.KafkaTopicProperties;
import org.springframework.cloud.stream.binder.kafka.utils.KafkaTopicUtils;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.cloud.stream.provisioning.ProvisioningException;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;
import org.springframework.retry.RetryOperations;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Kafka implementation for {@link ProvisioningProvider}.
 *
 * @author Soby Chacko
 * @author Gary Russell
 * @author Ilayaperumal Gopinathan
 * @author Simon Flandergan
 * @author Oleg Zhurakousky
 * @author Aldo Sinanaj
 */
public class KafkaTopicProvisioner implements ProvisioningProvider<ExtendedConsumerProperties<KafkaConsumerProperties>,
		ExtendedProducerProperties<KafkaProducerProperties>>, InitializingBean {

	private static final int DEFAULT_OPERATION_TIMEOUT = 30;

	private final Log logger = LogFactory.getLog(getClass());

	private final KafkaBinderConfigurationProperties configurationProperties;

	private final int operationTimeout = DEFAULT_OPERATION_TIMEOUT;

	private final Map<String, Object> adminClientProperties;

	private RetryOperations metadataRetryOperations;

	public KafkaTopicProvisioner(KafkaBinderConfigurationProperties kafkaBinderConfigurationProperties,
								KafkaProperties kafkaProperties) {
		Assert.isTrue(kafkaProperties != null, "KafkaProperties cannot be null");
		this.adminClientProperties = kafkaProperties.buildAdminProperties();
		this.configurationProperties = kafkaBinderConfigurationProperties;
		normalalizeBootPropsWithBinder(this.adminClientProperties, kafkaProperties, kafkaBinderConfigurationProperties);
	}

	/**
	 * Mutator for metadata retry operations.
	 *
	 * @param metadataRetryOperations the retry configuration
	 */
	public void setMetadataRetryOperations(RetryOperations metadataRetryOperations) {
		this.metadataRetryOperations = metadataRetryOperations;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.metadataRetryOperations == null) {
			RetryTemplate retryTemplate = new RetryTemplate();

			SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy();
			simpleRetryPolicy.setMaxAttempts(10);
			retryTemplate.setRetryPolicy(simpleRetryPolicy);

			ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
			backOffPolicy.setInitialInterval(100);
			backOffPolicy.setMultiplier(2);
			backOffPolicy.setMaxInterval(1000);
			retryTemplate.setBackOffPolicy(backOffPolicy);
			this.metadataRetryOperations = retryTemplate;
		}
	}

	@Override
	public ProducerDestination provisionProducerDestination(final String name,
			ExtendedProducerProperties<KafkaProducerProperties> properties) {

		if (this.logger.isInfoEnabled()) {
			this.logger.info("Using kafka topic for outbound: " + name);
		}
		KafkaTopicUtils.validateTopicName(name);
		try (AdminClient adminClient = AdminClient.create(this.adminClientProperties)) {
			createTopic(adminClient, name, properties.getPartitionCount(), false, properties.getExtension().getTopic());
			int partitions = 0;
			if (this.configurationProperties.isAutoCreateTopics()) {
				DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(Collections.singletonList(name));
				KafkaFuture<Map<String, TopicDescription>> all = describeTopicsResult.all();

				Map<String, TopicDescription> topicDescriptions = null;
				try {
					topicDescriptions = all.get(this.operationTimeout, TimeUnit.SECONDS);
				}
				catch (Exception ex) {
					throw new ProvisioningException("Problems encountered with partitions finding", ex);
				}
				TopicDescription topicDescription = topicDescriptions.get(name);
				partitions = topicDescription.partitions().size();
			}
			return new KafkaProducerDestination(name, partitions);
		}
	}

	@Override
	public ConsumerDestination provisionConsumerDestination(final String name, final String group,
			ExtendedConsumerProperties<KafkaConsumerProperties> properties) {
		if (!properties.isMultiplex()) {
			return doProvisionConsumerDestination(name, group, properties);
		}
		else {
			String[] destinations = StringUtils.commaDelimitedListToStringArray(name);
			for (String destination : destinations) {
				doProvisionConsumerDestination(destination.trim(), group, properties);
			}
			return new KafkaConsumerDestination(name);
		}
	}

	private ConsumerDestination doProvisionConsumerDestination(final String name, final String group,
			ExtendedConsumerProperties<KafkaConsumerProperties> properties) {

		if (properties.getExtension().isDestinationIsPattern()) {
			Assert.isTrue(!properties.getExtension().isEnableDlq(),
					"enableDLQ is not allowed when listening to topic patterns");
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Listening to a topic pattern - " + name
						+ " - no provisioning performed");
			}
			return new KafkaConsumerDestination(name);
		}
		KafkaTopicUtils.validateTopicName(name);
		boolean anonymous = !StringUtils.hasText(group);
		Assert.isTrue(!anonymous || !properties.getExtension().isEnableDlq(),
				"DLQ support is not available for anonymous subscriptions");
		if (properties.getInstanceCount() == 0) {
			throw new IllegalArgumentException("Instance count cannot be zero");
		}
		int partitionCount = properties.getInstanceCount() * properties.getConcurrency();
		ConsumerDestination consumerDestination = new KafkaConsumerDestination(name);
		try (AdminClient adminClient = createAdminClient()) {
			createTopic(adminClient, name, partitionCount, properties.getExtension().isAutoRebalanceEnabled(),
					properties.getExtension().getTopic());
			if (this.configurationProperties.isAutoCreateTopics()) {
				DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(Collections.singletonList(name));
				KafkaFuture<Map<String, TopicDescription>> all = describeTopicsResult.all();
				try {
					Map<String, TopicDescription> topicDescriptions = all.get(this.operationTimeout, TimeUnit.SECONDS);
					TopicDescription topicDescription = topicDescriptions.get(name);
					int partitions = topicDescription.partitions().size();
					consumerDestination = createDlqIfNeedBe(adminClient, name, group, properties, anonymous, partitions);
					if (consumerDestination == null) {
						consumerDestination = new KafkaConsumerDestination(name, partitions);
					}
				}
				catch (Exception ex) {
					throw new ProvisioningException("provisioning exception", ex);
				}
			}
		}
		return consumerDestination;
	}

	AdminClient createAdminClient() {
		return AdminClient.create(this.adminClientProperties);
	}

	/**
	 * In general, binder properties supersede boot kafka properties.
	 * The one exception is the bootstrap servers. In that case, we should only override
	 * the boot properties if (there is a binder property AND it is a non-default value)
	 * OR (if there is no boot property); this is needed because the binder property
	 * never returns a null value.
	 * @param adminProps the admin properties to normalize.
	 * @param bootProps the boot kafka properties.
	 * @param binderProps the binder kafka properties.
	 */
	private void normalalizeBootPropsWithBinder(Map<String, Object> adminProps, KafkaProperties bootProps,
			KafkaBinderConfigurationProperties binderProps) {
		// First deal with the outlier
		String kafkaConnectionString = binderProps.getKafkaConnectionString();
		if (ObjectUtils.isEmpty(adminProps.get(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG))
				|| !kafkaConnectionString.equals(binderProps.getDefaultKafkaConnectionString())) {
			adminProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConnectionString);
		}
		// Now override any boot values with binder values
		Map<String, String> binderProperties = binderProps.getConfiguration();
		Set<String> adminConfigNames = AdminClientConfig.configNames();
		binderProperties.forEach((key, value) -> {
			if (key.equals(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG)) {
				throw new IllegalStateException(
						"Set binder bootstrap servers via the 'brokers' property, not 'configuration'");
			}
			if (adminConfigNames.contains(key)) {
				Object replaced = adminProps.put(key, value);
				if (replaced != null && this.logger.isDebugEnabled()) {
					this.logger.debug("Overrode boot property: [" + key + "], from: [" + replaced + "] to: [" + value + "]");
				}
			}
		});
	}

	private ConsumerDestination createDlqIfNeedBe(AdminClient adminClient, String name, String group,
												ExtendedConsumerProperties<KafkaConsumerProperties> properties,
												boolean anonymous, int partitions) {
		if (properties.getExtension().isEnableDlq() && !anonymous) {
			String dlqTopic = StringUtils.hasText(properties.getExtension().getDlqName()) ?
					properties.getExtension().getDlqName() : "error." + name + "." + group;
			try {
				createTopicAndPartitions(adminClient, dlqTopic, partitions,
						properties.getExtension().isAutoRebalanceEnabled(), properties.getExtension().getTopic());
			}
			catch (Throwable throwable) {
				if (throwable instanceof Error) {
					throw (Error) throwable;
				}
				else {
					throw new ProvisioningException("provisioning exception", throwable);
				}
			}
			return new KafkaConsumerDestination(name, partitions, dlqTopic);
		}
		return null;
	}

	private void createTopic(AdminClient adminClient, String name, int partitionCount, boolean tolerateLowerPartitionsOnBroker,
			KafkaTopicProperties properties) {
		try {
			createTopicIfNecessary(adminClient, name, partitionCount, tolerateLowerPartitionsOnBroker, properties);
		}
		//TODO: Remove catching Throwable. See this thread: https://github.com/spring-cloud/spring-cloud-stream-binder-kafka/pull/514#discussion_r241075940
		catch (Throwable throwable) {
			if (throwable instanceof Error) {
				throw (Error) throwable;
			}
			else {
				//TODO: https://github.com/spring-cloud/spring-cloud-stream-binder-kafka/pull/514#discussion_r241075940
				throw new ProvisioningException("Provisioning exception", throwable);
			}
		}
	}

	private void createTopicIfNecessary(AdminClient adminClient, final String topicName, final int partitionCount,
			boolean tolerateLowerPartitionsOnBroker, KafkaTopicProperties properties) throws Throwable {

		if (this.configurationProperties.isAutoCreateTopics()) {
			createTopicAndPartitions(adminClient, topicName, partitionCount, tolerateLowerPartitionsOnBroker,
					properties);
		}
		else {
			this.logger.info("Auto creation of topics is disabled.");
		}
	}

	/**
	 * Creates a Kafka topic if needed, or try to increase its partition count to the
	 * desired number.
	 * @param adminClient kafka admin client
	 * @param topicName topic name
	 * @param partitionCount partition count
	 * @param tolerateLowerPartitionsOnBroker whether lower partitions count on broker is tolerated ot not
	 * @param topicProperties kafka topic properties
	 * @throws Throwable from topic creation
	 */
	private void createTopicAndPartitions(AdminClient adminClient, final String topicName, final int partitionCount,
			boolean tolerateLowerPartitionsOnBroker, KafkaTopicProperties topicProperties) throws Throwable {

		ListTopicsResult listTopicsResult = adminClient.listTopics();
		KafkaFuture<Set<String>> namesFutures = listTopicsResult.names();

		Set<String> names = namesFutures.get(this.operationTimeout, TimeUnit.SECONDS);
		if (names.contains(topicName)) {
			// only consider minPartitionCount for resizing if autoAddPartitions is true
			int effectivePartitionCount = this.configurationProperties.isAutoAddPartitions()
					? Math.max(this.configurationProperties.getMinPartitionCount(), partitionCount)
					: partitionCount;
			DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(Collections.singletonList(topicName));
			KafkaFuture<Map<String, TopicDescription>> topicDescriptionsFuture = describeTopicsResult.all();
			Map<String, TopicDescription> topicDescriptions = topicDescriptionsFuture.get(this.operationTimeout, TimeUnit.SECONDS);
			TopicDescription topicDescription = topicDescriptions.get(topicName);
			int partitionSize = topicDescription.partitions().size();
			if (partitionSize < effectivePartitionCount) {
				if (this.configurationProperties.isAutoAddPartitions()) {
					CreatePartitionsResult partitions = adminClient.createPartitions(
							Collections.singletonMap(topicName, NewPartitions.increaseTo(effectivePartitionCount)));
					partitions.all().get(this.operationTimeout, TimeUnit.SECONDS);
				}
				else if (tolerateLowerPartitionsOnBroker) {
					this.logger.warn("The number of expected partitions was: " + partitionCount + ", but "
							+ partitionSize + (partitionSize > 1 ? " have " : " has ") + "been found instead."
							+ "There will be " + (effectivePartitionCount - partitionSize) + " idle consumers");
				}
				else {
					throw new ProvisioningException("The number of expected partitions was: " + partitionCount + ", but "
							+ partitionSize + (partitionSize > 1 ? " have " : " has ") + "been found instead."
							+ "Consider either increasing the partition count of the topic or enabling " +
							"`autoAddPartitions`");
				}
			}
		}
		else {
			// always consider minPartitionCount for topic creation
			final int effectivePartitionCount = Math.max(this.configurationProperties.getMinPartitionCount(),
					partitionCount);
			this.metadataRetryOperations.execute((context) -> {

				NewTopic newTopic;
				Map<Integer, List<Integer>> replicasAssignments = topicProperties.getReplicasAssignments();
				if (replicasAssignments != null &&  replicasAssignments.size() > 0) {
					newTopic = new NewTopic(topicName, topicProperties.getReplicasAssignments());
				}
				else {
					newTopic = new NewTopic(topicName, effectivePartitionCount,
							topicProperties.getReplicationFactor() != null
									? topicProperties.getReplicationFactor()
									: this.configurationProperties.getReplicationFactor());
				}
				if (topicProperties.getProperties().size() > 0) {
					newTopic.configs(topicProperties.getProperties());
				}
				CreateTopicsResult createTopicsResult = adminClient.createTopics(Collections.singletonList(newTopic));
				try {
					createTopicsResult.all().get(this.operationTimeout, TimeUnit.SECONDS);
				}
				catch (Exception ex) {
					if (ex instanceof ExecutionException) {
						if (ex.getCause() instanceof TopicExistsException) {
							if (this.logger.isWarnEnabled()) {
								this.logger.warn("Attempt to create topic: " + topicName + ". Topic already exists.");
							}
						}
						else {
							this.logger.error("Failed to create topics", ex.getCause());
							throw ex.getCause();
						}
					}
					else {
						this.logger.error("Failed to create topics", ex.getCause());
						throw ex.getCause();
					}
				}
				return null;
			});
		}
	}

	public Collection<PartitionInfo> getPartitionsForTopic(final int partitionCount,
														final boolean tolerateLowerPartitionsOnBroker,
														final Callable<Collection<PartitionInfo>> callable,
														final String topicName) {
		try {
			return this.metadataRetryOperations
					.execute((context) -> {
						Collection<PartitionInfo> partitions = Collections.emptyList();

						try {
							//This call may return null or throw an exception.
							partitions = callable.call();
						}
						catch (Exception ex) {
							//The above call can potentially throw exceptions such as timeout. If we can determine
							//that the exception was due to an unknown topic on the broker, just simply rethrow that.
							if (ex instanceof UnknownTopicOrPartitionException) {
								throw ex;
							}
							this.logger.error("Failed to obtain partition information", ex);
						}
						if (CollectionUtils.isEmpty(partitions)) {
							try (AdminClient adminClient = AdminClient.create(this.adminClientProperties)) {
								final DescribeTopicsResult describeTopicsResult = adminClient
										.describeTopics(Collections.singletonList(topicName));
								describeTopicsResult.all().get();
							}
							catch (ExecutionException ex) {
								if (ex.getCause() instanceof UnknownTopicOrPartitionException) {
									throw (UnknownTopicOrPartitionException)ex.getCause();
								} else {
									logger.warn("No partitions have been retrieved for the topic (" + topicName + "). This will affect the health check.");
								}
							}
						}
						// do a sanity check on the partition set
						int partitionSize = partitions.size();
						if (partitionSize < partitionCount) {
							if (tolerateLowerPartitionsOnBroker) {
								this.logger.warn("The number of expected partitions was: " + partitionCount + ", but "
										+ partitionSize + (partitionSize > 1 ? " have " : " has ") + "been found instead."
										+ "There will be " + (partitionCount - partitionSize) + " idle consumers");
							}
							else {
								throw new IllegalStateException("The number of expected partitions was: "
										+ partitionCount + ", but " + partitionSize
										+ (partitionSize > 1 ? " have " : " has ") + "been found instead");
							}
						}
						return partitions;
					});
		}
		catch (Exception ex) {
			this.logger.error("Cannot initialize Binder", ex);
			throw new BinderException("Cannot initialize binder:", ex);
		}
	}

	private static final class KafkaProducerDestination implements ProducerDestination {

		private final String producerDestinationName;

		private final int partitions;

		KafkaProducerDestination(String destinationName, Integer partitions) {
			this.producerDestinationName = destinationName;
			this.partitions = partitions;
		}

		@Override
		public String getName() {
			return this.producerDestinationName;
		}

		@Override
		public String getNameForPartition(int partition) {
			return this.producerDestinationName;
		}

		@Override
		public String toString() {
			return "KafkaProducerDestination{" +
					"producerDestinationName='" + producerDestinationName + '\'' +
					", partitions=" + partitions +
					'}';
		}
	}

	private static final class KafkaConsumerDestination implements ConsumerDestination {

		private final String consumerDestinationName;

		private final int partitions;

		private final String dlqName;

		KafkaConsumerDestination(String consumerDestinationName) {
			this(consumerDestinationName, 0, null);
		}

		KafkaConsumerDestination(String consumerDestinationName, int partitions) {
			this(consumerDestinationName, partitions, null);
		}

		KafkaConsumerDestination(String consumerDestinationName, Integer partitions, String dlqName) {
			this.consumerDestinationName = consumerDestinationName;
			this.partitions = partitions;
			this.dlqName = dlqName;
		}

		@Override
		public String getName() {
			return this.consumerDestinationName;
		}

		@Override
		public String toString() {
			return "KafkaConsumerDestination{" +
					"consumerDestinationName='" + consumerDestinationName + '\'' +
					", partitions=" + partitions +
					", dlqName='" + dlqName + '\'' +
					'}';
		}
	}
}
