package com.board.api.features.post.application;

public interface PopularPostsScoreWriter {

	void incrementScore(long postId);
}
