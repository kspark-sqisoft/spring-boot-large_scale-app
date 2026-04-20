package com.board.api.features.auth.application;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.board.api.common.exception.ApiException;
import com.board.api.features.auth.api.dto.UserMeResponse;
import com.board.api.features.auth.domain.User;
import com.board.api.features.auth.infrastructure.persistence.UserRepository;
import com.board.api.features.file.domain.StoredFile;
import com.board.api.features.file.infrastructure.persistence.StoredFileRepository;
import lombok.RequiredArgsConstructor;

// /users/me 조회·수정 — User 엔티티와 API DTO 사이의 조립/검증 담당
@Service
@RequiredArgsConstructor
public class UserProfileService {

	private final UserRepository userRepository;
	private final StoredFileRepository storedFileRepository;

	@Transactional(readOnly = true)
	public UserMeResponse getMe(long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
		return UserMeResponse.fromUser(user);
	}

	@Transactional
	public UserMeResponse updateProfile(long userId, String displayName, String avatarFileIdRaw) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
		// PATCH 스타일: 둘 다 null이면 변경 없이 현재 상태 반환
		if (displayName == null && avatarFileIdRaw == null) {
			return UserMeResponse.fromUser(user);
		}
		if (displayName != null) {
			user.setDisplayName(displayName.isBlank() ? null : displayName.trim());
		}
		if (avatarFileIdRaw != null) {
			if (avatarFileIdRaw.isBlank()) {
				// 빈 문자열이면 아바타 제거
				user.setAvatarFileId(null);
			}
			else {
				long fid;
				try {
					fid = Long.parseLong(avatarFileIdRaw.trim());
				}
				catch (NumberFormatException ex) {
					throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_AVATAR_ID", "프로필 이미지 ID가 올바르지 않습니다.");
				}
				StoredFile file = storedFileRepository.findById(fid)
						.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "AVATAR_NOT_FOUND", "파일을 찾을 수 없습니다."));
				if (!file.getOwnerUserId().equals(userId)) {
					throw new ApiException(HttpStatus.FORBIDDEN, "AVATAR_FORBIDDEN", "본인이 업로드한 이미지만 사용할 수 있습니다.");
				}
				user.setAvatarFileId(fid);
			}
		}
		user.touchUpdatedAt();
		userRepository.save(user);
		return UserMeResponse.fromUser(user);
	}
}
