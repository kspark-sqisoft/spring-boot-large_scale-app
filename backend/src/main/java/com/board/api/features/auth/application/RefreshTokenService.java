package com.board.api.features.auth.application;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.board.api.common.id.SnowflakeIdGenerator;
import com.board.api.features.auth.domain.RefreshToken;
import com.board.api.features.auth.infrastructure.persistence.RefreshTokenRepository;

/**
 * 리프레시 토큰 발급·조회·폐기 서비스.
 * 보안 원칙: DB에는 원시 토큰이 아닌 SHA-256 해시값만 저장합니다.
 */
@Service
public class RefreshTokenService {

	private final RefreshTokenRepository refreshTokenRepository; // 리프레시 토큰 DB 조작
	private final SnowflakeIdGenerator idGenerator;              // 토큰 레코드 ID 생성
	private final long refreshExpirationDays;                    // 만료 일수 (JwtProperties에서 주입)

	// 생성자 주입: JwtProperties에서 만료 일수를 꺼내 필드에 저장
	public RefreshTokenService(
			RefreshTokenRepository refreshTokenRepository,
			SnowflakeIdGenerator idGenerator,
			com.board.api.common.config.JwtProperties jwtProperties) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.idGenerator = idGenerator;
		this.refreshExpirationDays = jwtProperties.getRefreshExpirationDays();
	}

	/**
	 * 새 리프레시 토큰 발급 후 DB 저장.
	 * rawToken의 SHA-256 해시를 계산해 저장 (원시값 저장 금지!)
	 */
	@Transactional
	public RefreshToken issue(long userId, String rawToken) {
		String hash = TokenHasher.sha256Hex(rawToken); // 원시값 → 해시
		Instant now = Instant.now();
		Instant expires = now.plus(refreshExpirationDays, ChronoUnit.DAYS); // 만료 시각 계산
		RefreshToken entity = new RefreshToken(
				idGenerator.nextId(), // Snowflake ID
				userId,
				hash,
				expires,
				now);
		return refreshTokenRepository.save(entity);
	}

	/**
	 * 원시 리프레시 토큰으로 유효한 DB 레코드 조회.
	 * - rawToken을 SHA-256 해시 후 DB에서 조회
	 * - 폐기(revoked=true)되지 않고 만료되지 않은 것만 반환
	 */
	@Transactional(readOnly = true) // readOnly: 읽기만 할 때 사용 → JPA가 변경 감지 생략해 성능 향상
	public Optional<RefreshToken> findValid(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			return Optional.empty(); // 빈 토큰 → 빈 결과 반환
		}
		String hash = TokenHasher.sha256Hex(rawToken);
		// 해시로 DB 조회 → 만료 시각 필터링
		return refreshTokenRepository.findByTokenHashAndRevokedIsFalse(hash)
				.filter(rt -> rt.getExpiresAt().isAfter(Instant.now())); // 아직 만료 안 됐는지 확인
	}

	// 이미 조회된 리프레시 토큰 엔티티를 폐기 (revoked=true로 변경)
	@Transactional
	public void revoke(RefreshToken token) {
		token.revoke(); // 엔티티 상태 변경 → @Transactional이 트랜잭션 종료 시 자동으로 UPDATE 실행
	}

	// 원시 토큰 문자열로 유효한 토큰을 찾아 폐기 (있으면 폐기, 없으면 아무것도 안 함)
	@Transactional
	public void revokeByRawToken(String rawToken) {
		findValid(rawToken).ifPresent(this::revoke);
	}
}
