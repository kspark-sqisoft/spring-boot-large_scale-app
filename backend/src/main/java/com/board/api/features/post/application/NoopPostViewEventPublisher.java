package com.board.api.features.post.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// Kafka 비활성 시 기본 구현 — 메시지 발행 없이 조회 API만 동작
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class NoopPostViewEventPublisher implements PostViewEventPublisher {

	@Override
	public void publishPostViewed(long postId) {
		// Kafka 꺼짐 — 외부 이벤트 없음
	}
}
