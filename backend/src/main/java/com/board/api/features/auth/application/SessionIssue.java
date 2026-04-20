package com.board.api.features.auth.application;

import java.util.Objects;

import com.board.api.features.auth.domain.User;

public record SessionIssue(
		User user,
		String accessToken,
		long expiresInSeconds,
		String rawRefreshToken
) {
	public SessionIssue {
		Objects.requireNonNull(user, "user");
		Objects.requireNonNull(accessToken, "accessToken");
		Objects.requireNonNull(rawRefreshToken, "rawRefreshToken");
	}
}
