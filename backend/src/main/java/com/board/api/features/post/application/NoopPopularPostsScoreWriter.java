package com.board.api.features.post.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// Redis 미사용(또는 false)일 때 선택되는 스텁 — 아무 것도 하지 않음
@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false", matchIfMissing = true)
public class NoopPopularPostsScoreWriter implements PopularPostsScoreWriter {

	@Override
	public void incrementScore(long postId) {
		// Redis 꺼짐 — 인기 순위 갱신 없음
	}
}
