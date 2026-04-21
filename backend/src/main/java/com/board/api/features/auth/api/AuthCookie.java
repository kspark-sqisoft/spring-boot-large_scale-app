package com.board.api.features.auth.api;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import org.springframework.http.ResponseCookie;

import com.board.api.common.security.JwtTokenProvider;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

public final class AuthCookie {

	public static final String REFRESH_COOKIE_NAME = "board_rt";

	private AuthCookie() {
	}

	public static Optional<String> readRefreshRaw(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return Optional.empty();
		}
		for (Cookie c : cookies) {
			if (REFRESH_COOKIE_NAME.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
				return Optional.of(c.getValue());
			}
		}
		return Optional.empty();
	}

	public static ResponseCookie refreshCookie(String rawToken, JwtTokenProvider jwtTokenProvider) {
		String token = Objects.requireNonNull(rawToken, "rawToken");
		long maxAgeSeconds = jwtTokenProvider.getRefreshExpirationDays() * 24L * 60L * 60L;
		Duration maxAge = Duration.ofSeconds(maxAgeSeconds);
		return ResponseCookie.from(REFRESH_COOKIE_NAME, token)
				.httpOnly(true)
				.secure(false)
				.path("/api/v1/auth")
				.maxAge(maxAge)
				.sameSite("Strict")
				.build();
	}

	public static ResponseCookie clearRefreshCookie() {
		return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
				.httpOnly(true)
				.secure(false)
				.path("/api/v1/auth")
				.maxAge(0)
				.sameSite("Strict")
				.build();
	}
}
