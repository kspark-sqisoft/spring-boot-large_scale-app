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

@Entity
@Table(name = "stored_files")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StoredFile {

	@Id
	private Long id;

	@Column(name = "owner_user_id", nullable = false)
	private Long ownerUserId;

	@Column(name = "content_type", nullable = false, length = 128)
	private String contentType;

	@Column(name = "original_name", nullable = false, length = 255)
	private String originalName;

	@Column(name = "size_bytes", nullable = false)
	private long sizeBytes;

	@Column(name = "storage_relative_path", nullable = false, length = 512)
	private String storageRelativePath;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
