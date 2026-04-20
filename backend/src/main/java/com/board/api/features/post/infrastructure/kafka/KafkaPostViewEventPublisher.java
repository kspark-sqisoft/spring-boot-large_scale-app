package com.board.api.features.post.infrastructure.kafka;

import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.board.api.common.config.KafkaBoardProperties;
import com.board.api.features.post.application.PostViewEventPublisher;
import com.board.api.features.post.application.event.PostViewedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// @Slf4j: Lombok이 SLF4J Logger 필드(log)를 자동 생성
@Slf4j
// @Component: PostViewEventPublisher 구현체로 스프링에 등록 → 컨트롤러 등에서 인터페이스 타입으로 주입 가능
@Component
// app.kafka.enabled=true 일 때만 이 Bean이 생성됨 (Kafka 자동설정이 꺼진 프로젝트에서 플래그로 켬)
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class KafkaPostViewEventPublisher implements PostViewEventPublisher {

	// Kafka 프로듀서를 감싼 스프링 헬퍼 (topic, key, value 전송)
	private final KafkaTemplate<String, String> kafkaTemplate;
	// 토픽 이름 등 Kafka 관련 설정값
	private final KafkaBoardProperties properties;
	// PostViewedEvent 객체 → JSON 문자열
	private final ObjectMapper objectMapper;

	@Override
	public void publishPostViewed(long postId) {
		try {
			String json = objectMapper.writeValueAsString(new PostViewedEvent(postId, Instant.now()));
			// 파티션 키로 postId 문자열 사용 → 같은 글의 이벤트가 같은 파티션으로 모일 확률↑(순서 보장에 유리)
			kafkaTemplate.send(properties.topicPostViewed(), Long.toString(postId), json);
		} catch (JsonProcessingException e) {
			log.warn("Failed to serialize post view event postId={}", postId, e);
		} catch (Exception e) {
			// 브로커 일시 장애 등: 조회 API 자체는 성공시키고 로그만 남김(이벤트 유실 가능성은 운영 정책에 따름)
			log.warn("Failed to publish post view event postId={}", postId, e);
		}
	}
}
