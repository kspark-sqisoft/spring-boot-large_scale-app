package com.board.api.features.post.api.dto;

import java.time.Instant;
import java.util.List;

import com.board.api.features.post.domain.Post;

public record PostResponse(
		String id,
		String authorUserId,
		String title,
		String content,
		Instant createdAt,
		Instant updatedAt,
		List<PostImageResponse> images,
		long likeCount,
		long commentCount,
		boolean likedByMe,
		long viewCount
) {

	public static PostResponse from(
			Post post,
			List<PostImageResponse> images,
			long likeCount,
			long commentCount,
			boolean likedByMe,
			long viewCount) {
		String author = post.getAuthorUserId() == null ? null : Long.toString(post.getAuthorUserId());
		return new PostResponse(
				Long.toString(post.getId()),
				author,
				post.getTitle(),
				post.getContent(),
				post.getCreatedAt(),
				post.getUpdatedAt(),
				images == null ? List.of() : images,
				likeCount,
				commentCount,
				likedByMe,
				viewCount);
	}
}
