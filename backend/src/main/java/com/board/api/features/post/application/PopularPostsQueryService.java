package com.board.api.features.post.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.board.api.features.post.api.dto.PostResponse;
import com.board.api.features.post.domain.Post;
import com.board.api.features.post.infrastructure.persistence.PostRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PopularPostsQueryService {

	private static final int MAX_LIMIT = 50;

	private final PostRepository postRepository;
	private final PostQueryService postQueryService;
	private final ObjectProvider<StringRedisTemplate> redisTemplate;

	@Transactional(readOnly = true)
	public List<PostResponse> listPopular(int limit, Long viewerUserId) {
		StringRedisTemplate redis = redisTemplate.getIfAvailable();
		if (redis == null) {
			return List.of();
		}
		int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
		Set<String> rawIds = redis.opsForZSet()
				.reverseRange(RedisPopularPostsScoreWriter.POPULAR_ZSET_KEY, 0, safeLimit - 1);
		if (rawIds == null || rawIds.isEmpty()) {
			return List.of();
		}
		List<Long> orderedIds = new ArrayList<>(rawIds.size());
		for (String id : rawIds) {
			try {
				orderedIds.add(Long.parseLong(id));
			} catch (NumberFormatException ignored) {
				// skip corrupt member
			}
		}
		if (orderedIds.isEmpty()) {
			return List.of();
		}
		List<Post> loaded = postRepository.findAllById(orderedIds);
		Map<Long, Post> byId = new HashMap<>();
		for (Post p : loaded) {
			byId.put(p.getId(), p);
		}
		return orderedIds.stream()
				.map(byId::get)
				.filter(Objects::nonNull)
				.map(p -> postQueryService.buildResponse(p, viewerUserId))
				.toList();
	}
}
