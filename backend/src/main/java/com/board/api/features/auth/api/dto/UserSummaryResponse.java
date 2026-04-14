package com.board.api.features.auth.api.dto;

import com.board.api.features.auth.domain.User;

public record UserSummaryResponse(
		String id,
		String email,
		String role,
		String createdAt
) {

	public static UserSummaryResponse from(User user) {
		return new UserSummaryResponse(
				Long.toString(user.getId()),
				user.getEmail(),
				user.getRole().name(),
				user.getCreatedAt().toString());
	}
}
