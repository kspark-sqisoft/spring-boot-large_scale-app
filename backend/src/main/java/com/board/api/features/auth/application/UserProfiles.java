package com.board.api.features.auth.application;

import com.board.api.features.auth.domain.User;

public final class UserProfiles {

	private UserProfiles() {
	}

	public static String resolveDisplayName(User user) {
		if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
			return user.getDisplayName().trim();
		}
		String email = user.getEmail();
		int at = email.indexOf('@');
		return at > 0 ? email.substring(0, at) : email;
	}
}
