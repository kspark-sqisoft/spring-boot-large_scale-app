package com.board.api.features.file.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 업로드된 파일의 메타데이터 도메인 엔티티.
 * DB의 "stored_files" 테이블과 매핑됩니다.
 *
 * 파일 자체는 디스크(data/uploads/)에 저장되고,
 * 이 엔티티는 파일 위치·이름·크기 등의 메타정보만 DB에 저장합니다.
 */
@Entity
@Table(name = "stored_files")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 내부용 기본 생성자
@AllArgsConstructor                                 // FileStorageService에서 생성 시 사용
public class StoredFile {

	@Id
	private Long id; // Snowflake ID (파일 다운로드 URL에도 사용: /api/v1/files/{id})

	@Column(name = "owner_user_id", nullable = false)
	private Long ownerUserId; // 이 파일을 업로드한 사용자 ID (본인 파일만 게시글에 첨부 가능)

	@Column(name = "content_type", nullable = false, length = 128)
	private String contentType; // MIME 타입 (예: "image/jpeg", "image/png")

	@Column(name = "original_name", nullable = false, length = 255)
	private String originalName; // 업로더가 올린 원래 파일명 (Content-Disposition 헤더에 사용)

	@Column(name = "size_bytes", nullable = false)
	private long sizeBytes; // 파일 크기(바이트) — 5MB(5*1024*1024) 초과 시 업로드 거부

	// 디스크 저장 상대 경로 (예: "12345/{fileId}_photo.jpg")
	// rootDirectory(FileStorageService)와 결합해 절대 경로 완성
	@Column(name = "storage_relative_path", nullable = false, length = 512)
	private String storageRelativePath;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt; // 업로드 시각
}
