package com.board.api.features.post.application;

import java.util.Collection;
import java.util.Map;

/**
 * 게시글 조회수. Redis 활성화 시 카운터, 비활성 시 항상 0.
 */
// 인터페이스: 구현체는 RedisPostViewService / NoopPostViewService 등이 @ConditionalOnProperty로 선택됨
public interface PostViewService {

	// 상세 조회 시 호출: 조회수 +1 후(또는 캐시 반영 후) 집계값 반환
	long incrementAndGet(long postId);

	// 단건 조회수 (목록 DTO 조립 등)
	long getCount(long postId);

	// 목록 N+1 방지: postId 여러 개에 대한 조회수를 한 번에
	Map<Long, Long> getCounts(Collection<Long> postIds);
}
