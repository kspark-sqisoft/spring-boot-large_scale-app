package com.board.api.features.comment.api.dto;

import java.time.Instant;

import com.board.api.features.comment.domain.Comment;

public record CommentResponse(
		String id,
		String postId,
		String parentCommentId,
		int depth,
		String content,
		CommentAuthorResponse author,
		Instant createdAt,
		Instant updatedAt
) {

	public static CommentResponse from(Comment comment, CommentAuthorResponse author) {
		return new CommentResponse(
				Long.toString(comment.getId()),
				Long.toString(comment.getPostId()),
				comment.getParentId() == null ? null : Long.toString(comment.getParentId()),
				comment.depth(),
				comment.getContent(),
				author,
				comment.getCreatedAt(),
				comment.getUpdatedAt());
	}
}
