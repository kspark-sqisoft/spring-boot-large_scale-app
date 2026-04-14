package com.board.api.features.comment.api.dto;

import java.util.List;

public record CommentListResponse(
		List<CommentResponse> comments
) {
}
