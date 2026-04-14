package com.board.api.features.post.api.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.board.api.features.post.domain.Post;

public record PostPageResponse(
		List<PostResponse> content,
		int page,
		int size,
		long totalElements,
		int totalPages
) {

	public static PostPageResponse from(Page<Post> page, java.util.function.Function<Post, PostResponse> mapper) {
		return new PostPageResponse(
				page.getContent().stream().map(mapper).toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages());
	}
}
