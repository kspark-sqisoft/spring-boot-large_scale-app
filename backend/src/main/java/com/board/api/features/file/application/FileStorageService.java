package com.board.api.features.file.application;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.board.api.common.config.FileStorageProperties;
import com.board.api.common.exception.ApiException;
import com.board.api.common.id.SnowflakeIdGenerator;
import com.board.api.features.file.domain.StoredFile;
import com.board.api.features.file.infrastructure.persistence.StoredFileRepository;

import jakarta.annotation.PostConstruct;

/** 디스크에 파일 저장·메타 DB 기록·이미지 타입·용량 검증 */
@Service
public class FileStorageService {

	private static final long MAX_IMAGE_BYTES = 5 * 1024 * 1024;

	private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
			"image/jpeg",
			"image/png",
			"image/gif",
			"image/webp");

	private final Path rootDirectory;
	private final StoredFileRepository storedFileRepository;
	private final SnowflakeIdGenerator idGenerator;

	public FileStorageService(
			FileStorageProperties properties,
			StoredFileRepository storedFileRepository,
			SnowflakeIdGenerator idGenerator) {
		this.rootDirectory = Path.of(properties.getDir()).toAbsolutePath().normalize();
		this.storedFileRepository = storedFileRepository;
		this.idGenerator = idGenerator;
	}

	@PostConstruct
	void ensureRootExists() throws IOException {
		Files.createDirectories(rootDirectory);
	}

	@Transactional
	public StoredFile storeImage(long ownerUserId, MultipartFile multipart) {
		if (multipart == null || multipart.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "FILE_EMPTY", "파일이 비어 있습니다.");
		}
		if (multipart.getSize() > MAX_IMAGE_BYTES) {
			throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "이미지는 5MB 이하여야 합니다.");
		}
		String contentType = multipart.getContentType();
		if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
			throw new ApiException(
					HttpStatus.BAD_REQUEST,
					"INVALID_IMAGE_TYPE",
					"허용 형식: JPEG, PNG, GIF, WebP");
		}
		String original = sanitizeOriginalName(multipart.getOriginalFilename());
		long fileId = idGenerator.nextId();
		String relative = ownerUserId + "/" + fileId + "_" + original;
		Path target = rootDirectory.resolve(relative).normalize();
		if (!target.startsWith(rootDirectory)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PATH", "잘못된 저장 경로입니다.");
		}
		try {
			Files.createDirectories(target.getParent());
			try (InputStream in = multipart.getInputStream()) {
				Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		catch (IOException ex) {
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_WRITE_FAILED", "파일 저장에 실패했습니다.");
		}
		Instant now = Instant.now();
		StoredFile entity = new StoredFile(
				fileId,
				ownerUserId,
				contentType,
				original,
				multipart.getSize(),
				relative,
				now);
		return storedFileRepository.save(entity);
	}

	public Path resolveAbsolutePath(StoredFile file) {
		return rootDirectory.resolve(file.getStorageRelativePath()).normalize();
	}

	private static String sanitizeOriginalName(String name) {
		if (name == null || name.isBlank()) {
			return "image";
		}
		String base = Path.of(name).getFileName().toString();
		base = base.replaceAll("[^a-zA-Z0-9._-]", "_");
		if (base.isBlank()) {
			base = "image";
		}
		if (base.length() > 120) {
			base = base.substring(0, 120);
		}
		return base;
	}
}
