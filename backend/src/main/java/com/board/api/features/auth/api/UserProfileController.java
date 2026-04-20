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
import lombok.RequiredArgsConstructor;

/** 현재 로그인 사용자 프로필 조회·수정({@code /me}) */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserProfileController {

	// 닉네임·아바타 등 사용자 프로필 도메인 로직
	private final UserProfileService userProfileService;

	// GET /api/v1/users/me — JWT의 userId로 DB에서 최신 프로필 조회
	@GetMapping("/me")
	@PreAuthorize("isAuthenticated()")
	public UserMeResponse me(@AuthenticationPrincipal AppUserDetails principal) {
		return userProfileService.getMe(principal.getUserId());
	}

	// PATCH /api/v1/users/me — 일부 필드만 수정 (PUT과 달리 전체 교체가 아님)
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
