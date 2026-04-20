package com.board.api.common.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.listener.ContainerProperties;

/** {@code app.kafka.enabled=true} 일 때만: 토픽·프로듀서·컨슈머 팩토리 등록 */
@Configuration
@EnableKafka
@EnableConfigurationProperties(KafkaBoardProperties.class)
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaBoardConfiguration {

	@Bean
	public NewTopic postViewedTopic(KafkaBoardProperties properties) {
		return TopicBuilder.name(properties.topicPostViewed()).partitions(1).replicas(1).build();
	}

	@Bean
	public KafkaAdmin kafkaAdmin(KafkaBoardProperties properties) {
		String bootstrapServers = Objects.requireNonNull(properties.bootstrapServers(), "bootstrapServers");
		Map<String, Object> config = new HashMap<>();
		config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		return new KafkaAdmin(Objects.requireNonNull(config));
	}

	@Bean
	public ProducerFactory<String, String> kafkaProducerFactory(KafkaBoardProperties properties) {
		String bootstrapServers = Objects.requireNonNull(properties.bootstrapServers(), "bootstrapServers");
		Map<String, Object> config = new HashMap<>();
		config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		config.put(ProducerConfig.ACKS_CONFIG, "1");
		return new DefaultKafkaProducerFactory<>(Objects.requireNonNull(config));
	}

	@Bean
	public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> kafkaProducerFactory) {
		return new KafkaTemplate<>(Objects.requireNonNull(kafkaProducerFactory, "kafkaProducerFactory"));
	}

	@Bean
	public ConsumerFactory<String, String> kafkaConsumerFactory(KafkaBoardProperties properties) {
		String bootstrapServers = Objects.requireNonNull(properties.bootstrapServers(), "bootstrapServers");
		String consumerGroup = Objects.requireNonNull(properties.consumerGroup(), "consumerGroup");
		Map<String, Object> config = new HashMap<>();
		config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		config.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);
		config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		return new DefaultKafkaConsumerFactory<>(Objects.requireNonNull(config));
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
			ConsumerFactory<String, String> kafkaConsumerFactory) {
		ConcurrentKafkaListenerContainerFactory<String, String> factory =
				new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(Objects.requireNonNull(kafkaConsumerFactory, "kafkaConsumerFactory"));
		factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
		return factory;
	}
}
