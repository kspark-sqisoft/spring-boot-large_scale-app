package com.board.api.features.post.application;

import java.util.Collection;
import java.util.Map;

/**
 * 게시글 조회수. Redis 활성화 시 카운터, 비활성 시 항상 0.
 */
public interface PostViewService {

	long incrementAndGet(long postId);

	long getCount(long postId);

	Map<Long, Long> getCounts(Collection<Long> postIds);
}
