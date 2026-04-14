package com.board.api.features.post.application;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
public class RedisPostViewService implements PostViewService {

	private static final String KEY_PREFIX = "board:views:post:";

	private final StringRedisTemplate redis;

	public RedisPostViewService(StringRedisTemplate redis) {
		this.redis = redis;
	}

	@Override
	public long incrementAndGet(long postId) {
		Long v = redis.opsForValue().increment(key(postId));
		return v != null ? v : 0L;
	}

	@Override
	public long getCount(long postId) {
		return parseLongOrZero(redis.opsForValue().get(key(postId)));
	}

	@Override
	public Map<Long, Long> getCounts(Collection<Long> postIds) {
		if (postIds.isEmpty()) {
			return Map.of();
		}
		List<Long> ordered = new ArrayList<>(postIds);
		List<String> keys = ordered.stream().map(this::key).toList();
		List<String> vals = redis.opsForValue().multiGet(keys);
		Map<Long, Long> out = new HashMap<>();
		for (int i = 0; i < ordered.size(); i++) {
			String raw = vals != null && i < vals.size() ? vals.get(i) : null;
			out.put(ordered.get(i), parseLongOrZero(raw));
		}
		return out;
	}

	private String key(long postId) {
		return KEY_PREFIX + postId;
	}

	private static long parseLongOrZero(String raw) {
		if (raw == null || raw.isBlank()) {
			return 0L;
		}
		try {
			return Long.parseLong(raw.trim());
		}
		catch (NumberFormatException ex) {
			return 0L;
		}
	}
}
