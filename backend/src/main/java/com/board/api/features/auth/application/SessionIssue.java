package com.board.api.features.auth.application;

import java.util.Objects;

import com.board.api.features.auth.domain.User;

// 로그인/회원가입 직후 컨트롤러에 넘기는 값 묶음 — record compact constructor로 null 방지
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
