package com.board.api.features.auth.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 리프레시 토큰 도메인 엔티티.
 * DB의 "refresh_tokens" 테이블에 저장됩니다.
 *
 * 보안 설계:
 * - 클라이언트에는 원시값(rawToken)을 쿠키로 전달
 * - DB에는 SHA-256 해시값만 저장 → DB가 유출돼도 직접 사용 불가
 * - 사용 후 폐기(revoke)해 재사용 방지 (토큰 로테이션)
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 내부용 기본 생성자
public class RefreshToken {

	@Id
	private Long id; // Snowflake ID

	@Column(name = "user_id", nullable = false)
	private Long userId; // 이 토큰을 소유한 사용자 ID

	// SHA-256 해시값 (64자 hex 문자열)
	// DB에 원시 토큰을 저장하지 않는 이유: DB 유출 시 리프레시 토큰 탈취 방지
	@Column(name = "token_hash", nullable = false, length = 64)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt; // 만료 시각 (기본 7일)

	// 로그아웃·토큰 갱신 시 true로 변경 → 이후 이 토큰으로 갱신 요청 시 거부
	@Column(nullable = false)
	private boolean revoked = false;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt; // 발급 시각

	// 서비스에서 새 리프레시 토큰 생성 시 사용하는 생성자
	public RefreshToken(Long id, Long userId, String tokenHash, Instant expiresAt, Instant createdAt) {
		this.id = id;
		this.userId = userId;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
		this.createdAt = createdAt;
	}

	// 토큰 폐기: 로그아웃 또는 갱신(로테이션) 시 기존 토큰 무효화
	public void revoke() {
		this.revoked = true;
	}
}
