package com.board.api.features.post.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
@RequiredArgsConstructor
public class RedisPopularPostsScoreWriter implements PopularPostsScoreWriter {

	public static final String POPULAR_ZSET_KEY = "board:popular:posts";

	private final StringRedisTemplate redis;

	@Override
	public void incrementScore(long postId) {
		redis.opsForZSet().incrementScore(POPULAR_ZSET_KEY, Long.toString(postId), 1.0);
	}
}
