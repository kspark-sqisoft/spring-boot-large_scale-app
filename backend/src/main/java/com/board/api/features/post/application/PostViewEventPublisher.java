package com.board.api.features.post.application;

// 게시글 조회를 다른 시스템에 알릴 때 사용 — Kafka 프로듀서 또는 No-op 구현
public interface PostViewEventPublisher {

	void publishPostViewed(long postId);
}
