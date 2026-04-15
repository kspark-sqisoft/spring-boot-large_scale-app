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

/** 인증 사용자 이미지 업로드 → 저장 후 {@code /api/v1/files/{id}} 로 노출 */
@RestController
@RequestMapping(FileApiPaths.UPLOADS)
public class UploadController {

	private final FileStorageService fileStorageService;

	public UploadController(FileStorageService fileStorageService) {
		this.fileStorageService = fileStorageService;
	}

	@PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("isAuthenticated()")
	public UploadResponse uploadImage(
			@AuthenticationPrincipal AppUserDetails principal,
			@RequestPart("file") MultipartFile file) {
		var stored = fileStorageService.storeImage(principal.getUserId(), file);
		return new UploadResponse(
				Long.toString(stored.getId()),
				FileApiPaths.FILES + "/" + stored.getId(),
				stored.getContentType());
	}
}
