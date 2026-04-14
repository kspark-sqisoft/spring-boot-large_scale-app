package com.board.api.features.file.api.dto;

public record UploadResponse(
		String id,
		String url,
		String contentType
) {
}
