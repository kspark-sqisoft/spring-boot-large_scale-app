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

// Redis ZSET에 쌓인 점수 순으로 인기 postId를 읽고, DB에서 Post를 다시 로드해 DTO로 만듦
@Service
@RequiredArgsConstructor
public class PopularPostsQueryService {

	// API로 한 번에 너무 많이 달라고 하면 Redis/DB 부하가 커지므로 상한
	private static final int MAX_LIMIT = 50;

	private final PostRepository postRepository;
	private final PostQueryService postQueryService;
	// Redis Bean이 없으면(getIfAvailable null) 인기글 기능 자체를 끔 — Optional Bean 패턴
	private final ObjectProvider<StringRedisTemplate> redisTemplate;

	@Transactional(readOnly = true)
	public List<PostResponse> listPopular(int limit, Long viewerUserId) {
		StringRedisTemplate redis = redisTemplate.getIfAvailable();
		if (redis == null) {
			// app.redis.enabled=false 이거나 Redis 설정 Bean이 없을 때
			return List.of();
		}
		int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
		// ZSET: 멤버=postId 문자열, score=조회 등으로 올린 인기 점수 — 높은 점수부터 safeLimit개
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
				// Redis에 잘못된 멤버가 섞인 경우 스킵
			}
		}
		if (orderedIds.isEmpty()) {
			return List.of();
		}
		// findAllById: IN 쿼리 한 번 — 순서는 보장되지 않아 아래 Map으로 원래 ZSET 순서 복원
		List<Post> loaded = postRepository.findAllById(orderedIds);
		Map<Long, Post> byId = new HashMap<>();
		for (Post p : loaded) {
			byId.put(p.getId(), p);
		}
		// ZSET에서 꺼낸 순서(인기 순)대로 스트림 정렬
		return orderedIds.stream()
				.map(byId::get)
				.filter(Objects::nonNull)
				.map(p -> postQueryService.buildResponse(p, viewerUserId))
				.toList();
	}
}
