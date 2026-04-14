package com.board.api.features.auth.application;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.board.api.common.id.SnowflakeIdGenerator;
import com.board.api.features.auth.domain.RefreshToken;
import com.board.api.features.auth.infrastructure.persistence.RefreshTokenRepository;

@Service
public class RefreshTokenService {

	private final RefreshTokenRepository refreshTokenRepository;
	private final SnowflakeIdGenerator idGenerator;
	private final long refreshExpirationDays;

	public RefreshTokenService(
			RefreshTokenRepository refreshTokenRepository,
			SnowflakeIdGenerator idGenerator,
			com.board.api.common.config.JwtProperties jwtProperties) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.idGenerator = idGenerator;
		this.refreshExpirationDays = jwtProperties.getRefreshExpirationDays();
	}

	@Transactional
	public RefreshToken issue(long userId, String rawToken) {
		String hash = TokenHasher.sha256Hex(rawToken);
		Instant now = Instant.now();
		Instant expires = now.plus(refreshExpirationDays, ChronoUnit.DAYS);
		RefreshToken entity = new RefreshToken(
				idGenerator.nextId(),
				userId,
				hash,
				expires,
				now);
		return refreshTokenRepository.save(entity);
	}

	@Transactional(readOnly = true)
	public Optional<RefreshToken> findValid(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			return Optional.empty();
		}
		String hash = TokenHasher.sha256Hex(rawToken);
		return refreshTokenRepository.findByTokenHashAndRevokedIsFalse(hash)
				.filter(rt -> rt.getExpiresAt().isAfter(Instant.now()));
	}

	@Transactional
	public void revoke(RefreshToken token) {
		token.revoke();
	}

	@Transactional
	public void revokeByRawToken(String rawToken) {
		findValid(rawToken).ifPresent(this::revoke);
	}
}
