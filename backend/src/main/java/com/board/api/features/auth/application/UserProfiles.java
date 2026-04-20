package com.board.api.features.auth.application;

import com.board.api.features.auth.domain.User;

// 프로필 표시명 규칙을 한곳에 모음 — 댓글·게시글 작성자 표시에 재사용
public final class UserProfiles {

	private UserProfiles() {
	}

	public static String resolveDisplayName(User user) {
		if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
			return user.getDisplayName().trim();
		}
		String email = user.getEmail();
		int at = email.indexOf('@');
		// 닉네임 없으면 이메일 @ 앞부분을 기본 표시명으로
		return at > 0 ? email.substring(0, at) : email;
	}
}
