package com.board.api.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import com.board.api.common.config.JwtProperties;
import com.board.api.features.auth.domain.User;
import com.board.api.features.auth.domain.UserRole;

/** 액세스 JWT 생성·파싱(짧은 만료). 리프레시는 이 클래스 밖에서 원시값+DB로 관리 */
@Component
public class JwtTokenProvider {

	private final JwtProperties jwtProperties;
	private final SecretKey secretKey;

	public JwtTokenProvider(JwtProperties jwtProperties) {
		this.jwtProperties = jwtProperties;
		this.secretKey = buildKey(jwtProperties.getSecret());
	}

	private static SecretKey buildKey(String secret) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
			return Keys.hmacShaKeyFor(digest);
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	public String createAccessToken(User user) {
		Date now = new Date();
		long expMs = jwtProperties.getAccessExpirationMinutes() * 60_000L;
		Date exp = new Date(now.getTime() + expMs);
		return Jwts.builder()
				.subject(Long.toString(user.getId()))
				.claim("email", user.getEmail())
				.claim("role", user.getRole().name())
				.issuedAt(now)
				.expiration(exp)
				.signWith(secretKey)
				.compact();
	}

	public Claims parseAccessToken(String token) {
		return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	public long getAccessExpirationSeconds() {
		return jwtProperties.getAccessExpirationMinutes() * 60L;
	}

	public long getRefreshExpirationDays() {
		return jwtProperties.getRefreshExpirationDays();
	}

	public UserRole parseRole(Claims claims) {
		return UserRole.valueOf(claims.get("role", String.class));
	}
}
