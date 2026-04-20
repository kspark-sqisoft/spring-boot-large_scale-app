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

/** 업로드된 파일 ID로 디스크 리소스 스트리밍(GET은 SecurityConfig에서 permitAll) */
@RestController
@RequestMapping(FileApiPaths.FILES)
public class FileDownloadController {

	private final StoredFileRepository storedFileRepository;
	private final FileStorageService fileStorageService;

	public FileDownloadController(
			StoredFileRepository storedFileRepository,
			FileStorageService fileStorageService) {
		this.storedFileRepository = storedFileRepository;
		this.fileStorageService = fileStorageService;
	}

	@GetMapping("/{fileId}")
	public ResponseEntity<Resource> download(@PathVariable long fileId) {
		StoredFile meta = storedFileRepository.findById(fileId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "파일을 찾을 수 없습니다."));
		Path path = fileStorageService.resolveAbsolutePath(meta);
		if (!Files.isRegularFile(path)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "FILE_MISSING", "저장소에서 파일을 찾을 수 없습니다.");
		}
		Resource body = new FileSystemResource(Objects.requireNonNull(path.toFile()));
		String contentType = Objects.requireNonNull(meta.getContentType(), "contentType");
		String originalName = Objects.requireNonNull(meta.getOriginalName(), "originalName");
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + originalName + "\"")
				.body(body);
	}
}
