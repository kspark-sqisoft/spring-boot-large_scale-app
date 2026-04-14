package com.board.api.features.post.application;

public interface PostViewEventPublisher {

	void publishPostViewed(long postId);
}
