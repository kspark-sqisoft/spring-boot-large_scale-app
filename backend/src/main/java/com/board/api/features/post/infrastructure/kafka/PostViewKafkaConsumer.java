package com.board.api.features.post.infrastructure.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.board.api.features.post.application.PopularPostsScoreWriter;
import com.board.api.features.post.application.event.PostViewedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class PostViewKafkaConsumer {

	// JSON 문자열 → PostViewedEvent 자바 객체
	private final ObjectMapper objectMapper;
	// Redis ZSET 등에 인기 점수를 올리는 구현체(또는 No-op)가 주입됨
	private final PopularPostsScoreWriter popularPostsScoreWriter;

	// @KafkaListener: 지정 토픽에 메시지가 들어오면 이 메서드를 백그라운드 스레드에서 호출
	// topics SpEL: yml에 없으면 기본값 board.post.viewed
	// containerFactory: KafkaBoardConfiguration에서 만든 리스너 컨테이너 설정 사용
	@KafkaListener(
			topics = "${app.kafka.topic-post-viewed:board.post.viewed}",
			containerFactory = "kafkaListenerContainerFactory")
	public void onPostViewed(String payload) {
		try {
			PostViewedEvent event = objectMapper.readValue(payload, PostViewedEvent.class);
			// 조회 1회당 점수 +1 같은 정책은 PopularPostsScoreWriter 구현에 따름
			popularPostsScoreWriter.incrementScore(event.postId());
		} catch (Exception e) {
			// 잘못된 JSON이어도 전체 컨슈머는 살리고 경고만 (DLQ 도입은 운영 규모에서 고려)
			log.warn("Failed to handle post viewed message: {}", payload, e);
		}
	}
}
