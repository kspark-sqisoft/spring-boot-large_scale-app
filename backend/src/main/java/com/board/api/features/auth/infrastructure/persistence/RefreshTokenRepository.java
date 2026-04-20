package com.board.api.features.auth.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.board.api.features.auth.domain.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	// 해시로 조회 후 서비스 레이어에서 만료 시각(expiresAt) 추가 필터
	Optional<RefreshToken> findByTokenHashAndRevokedIsFalse(String tokenHash);
}
