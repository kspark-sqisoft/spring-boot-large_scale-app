package com.board.api.features.post.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

// Redis가 켜져 있을 때만 Bean 등록 — Kafka 컨슈머 등에서 주입
@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
@RequiredArgsConstructor
public class RedisPopularPostsScoreWriter implements PopularPostsScoreWriter {

	// Sorted Set 키: 멤버=postId, score=누적 조회(또는 이벤트) 점수
	public static final String POPULAR_ZSET_KEY = "board:popular:posts";

	private final StringRedisTemplate redis;

	@Override
	public void incrementScore(long postId) {
		// ZINCRBY와 동일: 없던 멤버면 score 0에서 시작해 delta 만큼 증가
		redis.opsForZSet().incrementScore(POPULAR_ZSET_KEY, Long.toString(postId), 1.0);
	}
}
