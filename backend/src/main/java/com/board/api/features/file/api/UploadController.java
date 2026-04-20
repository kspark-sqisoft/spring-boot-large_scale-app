package com.board.api.features.file.api;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.board.api.common.security.AppUserDetails;
import com.board.api.features.file.api.dto.UploadResponse;
import com.board.api.features.file.application.FileStorageService;
import lombok.RequiredArgsConstructor;

/** 인증 사용자 이미지 업로드 → 저장 후 {@code /api/v1/files/{id}} 로 노출 */
@RestController
@RequestMapping(FileApiPaths.UPLOADS)
@RequiredArgsConstructor
public class UploadController {

	// 디스크(또는 스토리지)에 바이너리 저장 + DB에 StoredFile 메타데이터 기록
	private final FileStorageService fileStorageService;

	// POST multipart/form-data — 브라우저 <input type="file"> / FormData와 동일한 형식
	@PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("isAuthenticated()")
	public UploadResponse uploadImage(
			@AuthenticationPrincipal AppUserDetails principal,
			// @RequestPart("file"): 멀티파트에서 name="file" 인 파트를 MultipartFile로 받음
			@RequestPart("file") MultipartFile file) {
		// ownerUserId와 함께 저장해 이후 게시글 첨부 시 "본인 파일만" 연결 가능
		var stored = fileStorageService.storeImage(principal.getUserId(), file);
		return new UploadResponse(
				Long.toString(stored.getId()),
				FileApiPaths.FILES + "/" + stored.getId(),
				stored.getContentType());
	}
}
