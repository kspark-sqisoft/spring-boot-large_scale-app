package com.board.api.features.auth.application;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.board.api.common.exception.ApiException;
import com.board.api.features.auth.api.dto.UserMeResponse;
import com.board.api.features.auth.domain.User;
import com.board.api.features.auth.infrastructure.persistence.UserRepository;
import com.board.api.features.file.domain.StoredFile;
import com.board.api.features.file.api.FileApiPaths;
import com.board.api.features.file.infrastructure.persistence.StoredFileRepository;

@Service
public class UserProfileService {

	private final UserRepository userRepository;
	private final StoredFileRepository storedFileRepository;

	public UserProfileService(UserRepository userRepository, StoredFileRepository storedFileRepository) {
		this.userRepository = userRepository;
		this.storedFileRepository = storedFileRepository;
	}

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
		if (displayName == null && avatarFileIdRaw == null) {
			return UserMeResponse.fromUser(user);
		}
		if (displayName != null) {
			user.setDisplayName(displayName.isBlank() ? null : displayName.trim());
		}
		if (avatarFileIdRaw != null) {
			if (avatarFileIdRaw.isBlank()) {
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
