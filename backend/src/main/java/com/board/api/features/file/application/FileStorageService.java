package com.board.api.features.file.application;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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

	// 업로드 한 장당 최대 크기 (바이트)
	private static final long MAX_IMAGE_BYTES = 5 * 1024 * 1024;

	private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
			"image/jpeg",
			"image/png",
			"image/gif",
			"image/webp");

	// 각 포맷의 매직 바이트 시그니처 (Content-Type 위조 방지)
	private static final byte[] SIG_JPEG  = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };
	private static final byte[] SIG_PNG   = { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
	private static final byte[] SIG_GIF   = { 0x47, 0x49, 0x46, 0x38 }; // GIF8
	// WebP: bytes[0..3]="RIFF", bytes[8..11]="WEBP"
	private static final byte[] SIG_RIFF  = { 0x52, 0x49, 0x46, 0x46 };
	private static final byte[] SIG_WEBP  = { 0x57, 0x45, 0x42, 0x50 };
	private static final int    MAGIC_READ_BYTES = 12;

	private final Path rootDirectory;
	private final StoredFileRepository storedFileRepository;
	private final SnowflakeIdGenerator idGenerator;

	public FileStorageService(
			FileStorageProperties properties,
			StoredFileRepository storedFileRepository,
			SnowflakeIdGenerator idGenerator) {
		// normalize: ".." 등 제거해 절대 경로 기준 루트 고정
		this.rootDirectory = Path.of(properties.getDir()).toAbsolutePath().normalize();
		this.storedFileRepository = storedFileRepository;
		this.idGenerator = idGenerator;
	}

	// Bean 생성 직후 한 번 실행 — 업로드 루트 디렉터리가 없으면 생성
	@PostConstruct
	void ensureRootExists() throws IOException {
		Files.createDirectories(rootDirectory);
	}

	/**
	 * 이미지를 디스크에 저장하고 DB에 메타데이터를 기록한다.
	 *
	 * 순서: 검증 → 디스크 쓰기 → DB 저장
	 * DB 저장 실패 시 디스크 파일을 정리해 고아 파일 발생을 방지한다.
	 * (@Transactional 제거: DB 트랜잭션으로 디스크 I/O를 감쌀 수 없기 때문)
	 */
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

		// 매직 바이트 검증: Content-Type 위조 방지
		byte[] header;
		byte[] fullBytes;
		try (InputStream in = multipart.getInputStream()) {
			fullBytes = in.readAllBytes();
		}
		catch (IOException ex) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "FILE_READ_FAILED", "파일을 읽을 수 없습니다.");
		}
		header = Arrays.copyOf(fullBytes, Math.min(fullBytes.length, MAGIC_READ_BYTES));
		if (!isValidImageSignature(contentType.toLowerCase(Locale.ROOT), header)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IMAGE_SIGNATURE", "파일 형식이 Content-Type과 일치하지 않습니다.");
		}

		String original = sanitizeOriginalName(multipart.getOriginalFilename());
		long fileId = idGenerator.nextId();
		String relative = ownerUserId + "/" + fileId + "_" + original;
		Path target = rootDirectory.resolve(relative).normalize();
		if (!target.startsWith(rootDirectory)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PATH", "잘못된 저장 경로입니다.");
		}

		// 1단계: 디스크에 먼저 쓴다
		try {
			Files.createDirectories(target.getParent());
			Files.write(target, fullBytes);
		}
		catch (IOException ex) {
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_WRITE_FAILED", "파일 저장에 실패했습니다.");
		}

		// 2단계: DB에 메타데이터 저장 — 실패 시 디스크 파일 정리
		try {
			StoredFile entity = new StoredFile(
					fileId,
					ownerUserId,
					contentType,
					original,
					multipart.getSize(),
					relative,
					Instant.now());
			return storedFileRepository.save(entity);
		}
		catch (Exception ex) {
			try {
				Files.deleteIfExists(target);
			}
			catch (IOException ignored) {
				// 정리 실패는 로그로만 — 원래 예외를 우선 전파
			}
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_METADATA_FAILED", "파일 메타데이터 저장에 실패했습니다.");
		}
	}

	// 다운로드 컨트롤러에서 실제 파일 시스템 경로 조합
	public Path resolveAbsolutePath(StoredFile file) {
		return rootDirectory.resolve(file.getStorageRelativePath()).normalize();
	}

	// 매직 바이트로 실제 이미지 형식 검증 (Content-Type 위조 방지)
	private static boolean isValidImageSignature(String contentType, byte[] header) {
		return switch (contentType) {
			case "image/jpeg" -> startsWith(header, SIG_JPEG);
			case "image/png"  -> startsWith(header, SIG_PNG);
			case "image/gif"  -> startsWith(header, SIG_GIF);
			case "image/webp" -> header.length >= 12
					&& startsWith(header, SIG_RIFF)
					&& Arrays.equals(Arrays.copyOfRange(header, 8, 12), SIG_WEBP);
			default -> false;
		};
	}

	private static boolean startsWith(byte[] data, byte[] prefix) {
		if (data.length < prefix.length) {
			return false;
		}
		for (int i = 0; i < prefix.length; i++) {
			if (data[i] != prefix[i]) {
				return false;
			}
		}
		return true;
	}

	// 경로 구분자·특수문자 제거로 OS·웹에서 안전한 파일명만 남김
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
