package com.board.api.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.board.api.common.config.JwtProperties;
import com.board.api.features.auth.domain.User;
import com.board.api.features.auth.domain.UserRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

class JwtTokenProviderTest {

	private JwtTokenProvider provider;

	@BeforeEach
	void setUp() {
		JwtProperties props = new JwtProperties();
		props.setSecret("unit-test-jwt-secret-must-be-long-enough-for-hmac-sha256");
		props.setAccessExpirationMinutes(60);
		provider = new JwtTokenProvider(props);
	}

	@Test
	void createAccessToken_roundTrips_subject_email_role() {
		User user = User.create(9_001L, "jwt-tester@example.com", "hash", UserRole.ADMIN);
		String token = provider.createAccessToken(user);
		Claims claims = provider.parseAccessToken(token);
		assertThat(claims.getSubject()).isEqualTo("9001");
		assertThat(claims.get("email", String.class)).isEqualTo("jwt-tester@example.com");
		assertThat(provider.parseRole(claims)).isEqualTo(UserRole.ADMIN);
	}

	@Test
	void parseAccessToken_rejects_tampered_token() {
		User user = User.create(1L, "a@b.com", "h", UserRole.USER);
		String token = provider.createAccessToken(user);
		String tampered = token.substring(0, token.length() - 3) + "xxx";
		assertThatThrownBy(() -> provider.parseAccessToken(tampered))
				.isInstanceOf(JwtException.class);
	}
}
