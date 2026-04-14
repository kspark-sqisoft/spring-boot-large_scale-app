package com.board.api.features.post.api.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
		@NotBlank @Size(max = 500) String title,
		@NotBlank @Size(max = 10_000) String content,
		@Size(max = 10) List<String> imageFileIds
) {
}
