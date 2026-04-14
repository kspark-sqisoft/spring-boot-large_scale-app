package com.board.api.features.auth.api.dto;

public record AccessTokenResponse(
		String accessToken,
		long expiresInSeconds,
		String tokenType
) {

	public static AccessTokenResponse of(String accessToken, long expiresInSeconds) {
		return new AccessTokenResponse(accessToken, expiresInSeconds, "Bearer");
	}
}
