package com.board.api.features.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.board.api.common.config.JwtProperties;
import com.board.api.common.id.SnowflakeIdGenerator;
import com.board.api.features.auth.domain.RefreshToken;
import com.board.api.features.auth.infrastructure.persistence.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

	@Mock
	private RefreshTokenRepository refreshTokenRepository;
	@Mock
	private SnowflakeIdGenerator idGenerator;

	private RefreshTokenService refreshTokenService;

	@BeforeEach
	void setUp() {
		JwtProperties jwtProperties = new JwtProperties();
		jwtProperties.setRefreshExpirationDays(7);
		refreshTokenService = new RefreshTokenService(refreshTokenRepository, idGenerator, jwtProperties);
	}

	@Test
	void issue_persists_sha256_of_raw_token() {
		when(idGenerator.nextId()).thenReturn(77L);
		when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

		RefreshToken saved = refreshTokenService.issue(10L, "opaque-refresh-raw");

		ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository).save(captor.capture());
		RefreshToken entity = captor.getValue();
		assertThat(entity.getUserId()).isEqualTo(10L);
		assertThat(entity.getTokenHash()).isEqualTo(TokenHasher.sha256Hex("opaque-refresh-raw"));
		assertThat(saved.getTokenHash()).isEqualTo(entity.getTokenHash());
	}

	@Test
	void findValid_returns_empty_for_blank_raw() {
		assertThat(refreshTokenService.findValid(null)).isEmpty();
		assertThat(refreshTokenService.findValid("   ")).isEmpty();
	}

	@Test
	void findValid_filters_expired() {
		Instant past = Instant.now().minus(1, ChronoUnit.HOURS);
		RefreshToken expired = new RefreshToken(1L, 1L, TokenHasher.sha256Hex("x"), past, past.minus(1, ChronoUnit.DAYS));
		when(refreshTokenRepository.findByTokenHashAndRevokedIsFalse(TokenHasher.sha256Hex("x")))
				.thenReturn(java.util.Optional.of(expired));

		assertThat(refreshTokenService.findValid("x")).isEmpty();
	}
}
