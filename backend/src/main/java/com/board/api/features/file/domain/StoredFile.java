package com.board.api.features.file.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stored_files")
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

	protected StoredFile() {
	}

	public StoredFile(
			Long id,
			Long ownerUserId,
			String contentType,
			String originalName,
			long sizeBytes,
			String storageRelativePath,
			Instant createdAt) {
		this.id = id;
		this.ownerUserId = ownerUserId;
		this.contentType = contentType;
		this.originalName = originalName;
		this.sizeBytes = sizeBytes;
		this.storageRelativePath = storageRelativePath;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public Long getOwnerUserId() {
		return ownerUserId;
	}

	public String getContentType() {
		return contentType;
	}

	public String getOriginalName() {
		return originalName;
	}

	public long getSizeBytes() {
		return sizeBytes;
	}

	public String getStorageRelativePath() {
		return storageRelativePath;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
