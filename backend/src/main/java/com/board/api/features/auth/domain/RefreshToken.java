package com.board.api.features.auth.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

	@Id
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "token_hash", nullable = false, length = 64)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(nullable = false)
	private boolean revoked = false;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected RefreshToken() {
	}

	public RefreshToken(Long id, Long userId, String tokenHash, Instant expiresAt, Instant createdAt) {
		this.id = id;
		this.userId = userId;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public boolean isRevoked() {
		return revoked;
	}

	public void revoke() {
		this.revoked = true;
	}
}
