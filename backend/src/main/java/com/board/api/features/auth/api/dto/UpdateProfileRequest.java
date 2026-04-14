package com.board.api.features.auth.api.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
		@Size(max = 100) String displayName,
		String avatarFileId
) {
}
