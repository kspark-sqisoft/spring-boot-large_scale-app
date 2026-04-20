package com.board.api.features.file.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.board.api.common.exception.ApiException;
import com.board.api.features.file.application.FileStorageService;
import com.board.api.features.file.domain.StoredFile;
import com.board.api.features.file.infrastructure.persistence.StoredFileRepository;
import lombok.RequiredArgsConstructor;

/** 업로드된 파일 ID로 디스크 리소스 스트리밍(GET은 SecurityConfig에서 permitAll) */
@RestController
@RequestMapping(FileApiPaths.FILES)
@RequiredArgsConstructor
public class FileDownloadController {

	// DB에 저장된 파일 메타데이터(경로 상대값, MIME, 원본 파일명)
	private final StoredFileRepository storedFileRepository;
	// 설정(app.upload.*) 기준으로 실제 OS 파일 경로 조합
	private final FileStorageService fileStorageService;

	// GET /api/v1/files/{fileId} — 브라우저에서 <img src="..."> 로 바로 열 수 있음
	@GetMapping("/{fileId}")
	public ResponseEntity<Resource> download(@PathVariable long fileId) {
		StoredFile meta = storedFileRepository.findById(fileId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "파일을 찾을 수 없습니다."));
		Path path = fileStorageService.resolveAbsolutePath(meta);
		if (!Files.isRegularFile(path)) {
			// DB에는 있는데 디스크에서 삭제된 경우 등
			throw new ApiException(HttpStatus.NOT_FOUND, "FILE_MISSING", "저장소에서 파일을 찾을 수 없습니다.");
		}
		// FileSystemResource: 스프링이 InputStreamResource 대신 파일 기반 스트리밍 최적화 가능
		Resource body = new FileSystemResource(Objects.requireNonNull(path.toFile()));
		String contentType = Objects.requireNonNull(meta.getContentType(), "contentType");
		String originalName = Objects.requireNonNull(meta.getOriginalName(), "originalName");
		return ResponseEntity.ok()
				// Content-Type: 브라우저가 이미지/바이너리 해석
				.contentType(MediaType.parseMediaType(contentType))
				// inline: 다운로드 대신 페이지 안에 표시(attachment면 저장 대화상자 유도)
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + originalName + "\"")
				.body(body);
	}
}
