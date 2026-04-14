package com.board.api.features.auth.application;

import com.board.api.features.auth.domain.User;

public record SessionIssue(
		User user,
		String accessToken,
		long expiresInSeconds,
		String rawRefreshToken
) {
}
