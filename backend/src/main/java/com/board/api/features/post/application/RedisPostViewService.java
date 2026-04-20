package com.board.api.features.post.application;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

// StringRedisTemplate: Redis에 문자열 값으로 카운터 저장(INCR)
@Service
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
@RequiredArgsConstructor
public class RedisPostViewService implements PostViewService {

	private static final String KEY_PREFIX = "board:views:post:";

	private final StringRedisTemplate redis;

	@Override
	public long incrementAndGet(long postId) {
		// INCR: 원자적으로 +1 후 새 값 반환
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
		// MGET에 가까운 배치 조회 — 루프마다 GET 하지 않음
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
