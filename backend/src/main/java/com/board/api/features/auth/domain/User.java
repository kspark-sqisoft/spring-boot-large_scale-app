package com.board.api.features.auth.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

	@Id
	private Long id;

	@Column(nullable = false, unique = true, length = 255)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private UserRole role;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "display_name", length = 100)
	private String displayName;

	@Column(name = "avatar_file_id")
	private Long avatarFileId;

	protected User() {
	}

	public User(
			Long id,
			String email,
			String passwordHash,
			UserRole role,
			Instant createdAt,
			Instant updatedAt,
			String displayName,
			Long avatarFileId) {
		this.id = id;
		this.email = email;
		this.passwordHash = passwordHash;
		this.role = role;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.displayName = displayName;
		this.avatarFileId = avatarFileId;
	}

	public static User create(Long id, String email, String passwordHash, UserRole role) {
		Instant now = Instant.now();
		return new User(id, email, passwordHash, role, now, now, null, null);
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public UserRole getRole() {
		return role;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public void touchUpdatedAt() {
		this.updatedAt = Instant.now();
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public Long getAvatarFileId() {
		return avatarFileId;
	}

	public void setAvatarFileId(Long avatarFileId) {
		this.avatarFileId = avatarFileId;
	}
}
