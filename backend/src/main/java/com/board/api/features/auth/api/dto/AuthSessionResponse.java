package com.board.api.features.auth.api.dto;

import com.board.api.features.auth.application.SessionIssue;

public record AuthSessionResponse(
		UserMeResponse user,
		String accessToken,
		long expiresInSeconds,
		String tokenType
) {

	public static AuthSessionResponse from(SessionIssue issue) {
		return new AuthSessionResponse(
				UserMeResponse.fromUser(issue.user()),
				issue.accessToken(),
				issue.expiresInSeconds(),
				"Bearer");
	}
}
