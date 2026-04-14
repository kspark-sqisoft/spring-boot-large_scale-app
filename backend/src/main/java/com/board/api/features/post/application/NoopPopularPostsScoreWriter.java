package com.board.api.features.post.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false", matchIfMissing = true)
public class NoopPopularPostsScoreWriter implements PopularPostsScoreWriter {

	@Override
	public void incrementScore(long postId) {
		// Redis disabled — popular ranking not updated
	}
}
