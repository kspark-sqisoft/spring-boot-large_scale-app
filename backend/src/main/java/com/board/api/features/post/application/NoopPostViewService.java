package com.board.api.features.post.application;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false", matchIfMissing = true)
public class NoopPostViewService implements PostViewService {

	@Override
	public long incrementAndGet(long postId) {
		return 0L;
	}

	@Override
	public long getCount(long postId) {
		return 0L;
	}

	@Override
	public Map<Long, Long> getCounts(Collection<Long> postIds) {
		Map<Long, Long> m = new HashMap<>();
		for (Long id : postIds) {
			m.put(id, 0L);
		}
		return m;
	}
}
