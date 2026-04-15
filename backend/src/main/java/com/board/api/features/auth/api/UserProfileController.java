package com.board.api.features.auth.api;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.board.api.common.security.AppUserDetails;
import com.board.api.features.auth.api.dto.UpdateProfileRequest;
import com.board.api.features.auth.api.dto.UserMeResponse;
import com.board.api.features.auth.application.UserProfileService;

import jakarta.validation.Valid;

/** 현재 로그인 사용자 프로필 조회·수정({@code /me}) */
@RestController
@RequestMapping("/api/v1/users")
public class UserProfileController {

	private final UserProfileService userProfileService;

	public UserProfileController(UserProfileService userProfileService) {
		this.userProfileService = userProfileService;
	}

	@GetMapping("/me")
	@PreAuthorize("isAuthenticated()")
	public UserMeResponse me(@AuthenticationPrincipal AppUserDetails principal) {
		return userProfileService.getMe(principal.getUserId());
	}

	@PatchMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("isAuthenticated()")
	public UserMeResponse patchMe(
			@AuthenticationPrincipal AppUserDetails principal,
			@Valid @RequestBody UpdateProfileRequest request) {
		return userProfileService.updateProfile(
				principal.getUserId(),
				request.displayName(),
				request.avatarFileId());
	}
}
