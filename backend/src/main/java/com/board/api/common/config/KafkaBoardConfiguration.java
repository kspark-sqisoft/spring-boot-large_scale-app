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

/**
 * Kafka 관련 Bean들을 등록하는 설정 클래스.
 * {@code app.kafka.enabled=true}일 때만 활성화됩니다.
 * Kafka: 분산 메시지 큐. 게시글 조회 이벤트를 비동기로 발행·소비합니다.
 */
// @Configuration: @Bean 메서드를 Spring이 실행해 Bean 등록
// @EnableKafka: Spring Kafka의 @KafkaListener 등 기능 활성화
// @EnableConfigurationProperties: KafkaBoardProperties를 Bean으로 등록해 주입 가능하게 함
// @ConditionalOnProperty: app.kafka.enabled=true 일 때만 이 설정 클래스 전체를 활성화
@Configuration
@EnableKafka
@EnableConfigurationProperties(KafkaBoardProperties.class)
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaBoardConfiguration {

	/**
	 * Kafka 토픽 자동 생성.
	 * 앱 기동 시 "board.post.viewed" 토픽이 없으면 자동으로 만들어줌.
	 * partitions=1: 파티션 1개 (단일 서버 개발용)
	 * replicas=1: 복제본 1개 (운영에서는 3+ 권장)
	 */
	@Bean
	public NewTopic postViewedTopic(KafkaBoardProperties properties) {
		return TopicBuilder.name(properties.topicPostViewed()).partitions(1).replicas(1).build();
	}

	/**
	 * Kafka 관리자 클라이언트: 토픽 생성·삭제 등 관리 작업에 사용.
	 */
	@Bean
	public KafkaAdmin kafkaAdmin(KafkaBoardProperties properties) {
		String bootstrapServers = Objects.requireNonNull(properties.bootstrapServers(), "bootstrapServers");
		Map<String, Object> config = new HashMap<>();
		config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers); // 브로커 주소
		return new KafkaAdmin(Objects.requireNonNull(config));
	}

	/**
	 * Kafka 프로듀서(메시지 발행자) 팩토리.
	 * 게시글 조회 이벤트를 JSON 문자열로 Kafka 토픽에 발행.
	 */
	@Bean
	public ProducerFactory<String, String> kafkaProducerFactory(KafkaBoardProperties properties) {
		String bootstrapServers = Objects.requireNonNull(properties.bootstrapServers(), "bootstrapServers");
		Map<String, Object> config = new HashMap<>();
		config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);   // 키: 문자열
		config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class); // 값: 문자열
		config.put(ProducerConfig.ACKS_CONFIG, "1"); // 리더 파티션에만 쓰면 성공으로 처리 (성능↑ 안전성↓)
		return new DefaultKafkaProducerFactory<>(Objects.requireNonNull(config));
	}

	/**
	 * KafkaTemplate: 프로듀서를 쉽게 사용하는 Spring 래퍼.
	 * kafkaTemplate.send(topic, key, value) 한 줄로 메시지 발행 가능.
	 */
	@Bean
	public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> kafkaProducerFactory) {
		return new KafkaTemplate<>(Objects.requireNonNull(kafkaProducerFactory, "kafkaProducerFactory"));
	}

	/**
	 * Kafka 컨슈머(메시지 소비자) 팩토리.
	 * board.post.viewed 토픽의 조회 이벤트를 구독해 Redis 인기글 점수 업데이트에 사용.
	 */
	@Bean
	public ConsumerFactory<String, String> kafkaConsumerFactory(KafkaBoardProperties properties) {
		String bootstrapServers = Objects.requireNonNull(properties.bootstrapServers(), "bootstrapServers");
		String consumerGroup = Objects.requireNonNull(properties.consumerGroup(), "consumerGroup");
		Map<String, Object> config = new HashMap<>();
		config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		config.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);                             // 컨슈머 그룹
		config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);   // 키 역직렬화
		config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class); // 값 역직렬화
		config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // 처음 구독 시 가장 오래된 메시지부터 읽음
		return new DefaultKafkaConsumerFactory<>(Objects.requireNonNull(config));
	}

	/**
	 * @KafkaListener 메서드를 실행할 컨테이너 팩토리.
	 * AckMode.RECORD: 메시지 1개 처리 완료 시 즉시 오프셋 커밋 (처리 순서 보장).
	 */
	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
			ConsumerFactory<String, String> kafkaConsumerFactory) {
		ConcurrentKafkaListenerContainerFactory<String, String> factory =
				new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(Objects.requireNonNull(kafkaConsumerFactory, "kafkaConsumerFactory"));
		// RECORD 모드: 레코드(메시지) 하나 처리 후 즉시 오프셋 커밋
		factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
		return factory;
	}
}
