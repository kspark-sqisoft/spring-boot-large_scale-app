package com.board.api.features.auth.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
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

	public static User create(Long id, String email, String passwordHash, UserRole role) {
		Instant now = Instant.now();
		return new User(id, email, passwordHash, role, now, now, null, null);
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public void touchUpdatedAt() {
		this.updatedAt = Instant.now();
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public void setAvatarFileId(Long avatarFileId) {
		this.avatarFileId = avatarFileId;
	}
}
