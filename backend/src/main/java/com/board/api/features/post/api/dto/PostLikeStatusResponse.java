package com.board.api.features.post.api.dto;

public record PostLikeStatusResponse(
		long likeCount,
		boolean likedByMe
) {
}
