package com.board.api.features.auth.api.dto;

import com.board.api.common.security.AppUserDetails;
import com.board.api.features.auth.application.UserProfiles;
import com.board.api.features.auth.domain.User;
import com.board.api.features.file.api.FileApiPaths;

public record UserMeResponse(
		String id,
		String email,
		String role,
		String displayName,
		String avatarUrl
) {

	public static UserMeResponse fromUser(User user) {
		String avatar = user.getAvatarFileId() != null
				? FileApiPaths.FILES + "/" + user.getAvatarFileId()
				: null;
		return new UserMeResponse(
				Long.toString(user.getId()),
				user.getEmail(),
				user.getRole().name(),
				UserProfiles.resolveDisplayName(user),
				avatar);
	}

	/**
	 * JWT만 있는 경우(DB 조회 전) 최소 정보.
	 */
	public static UserMeResponse fromPrincipal(AppUserDetails principal) {
		return new UserMeResponse(
				Long.toString(principal.getUserId()),
				principal.getUsername(),
				principal.getRole().name(),
				fallbackNameFromEmail(principal.getUsername()),
				null);
	}

	private static String fallbackNameFromEmail(String email) {
		int at = email.indexOf('@');
		return at > 0 ? email.substring(0, at) : email;
	}
}
