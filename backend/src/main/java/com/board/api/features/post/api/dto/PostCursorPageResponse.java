package com.board.api.features.post.api.dto;

import java.util.List;

public record PostCursorPageResponse(
		List<PostResponse> content,
		String nextCursor,
		int size,
		boolean hasNext
) {
}
