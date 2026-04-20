package com.board.api.features.post.application;

// 조회 이벤트 등에서 인기 점수를 올릴 때 사용 — Redis 구현 또는 No-op 구현이 주입됨
public interface PopularPostsScoreWriter {

	void incrementScore(long postId);
}
