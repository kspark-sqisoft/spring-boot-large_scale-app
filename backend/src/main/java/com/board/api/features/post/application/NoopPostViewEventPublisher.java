package com.board.api.features.post.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class NoopPostViewEventPublisher implements PostViewEventPublisher {

	@Override
	public void publishPostViewed(long postId) {
		// Kafka disabled — no outbound event
	}
}
