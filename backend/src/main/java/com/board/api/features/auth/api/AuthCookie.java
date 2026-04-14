package com.board.api.features.auth.api;

import java.time.Duration;
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
		long maxAgeSeconds = jwtTokenProvider.getRefreshExpirationDays() * 24 * 60 * 60;
		return ResponseCookie.from(REFRESH_COOKIE_NAME, rawToken)
				.httpOnly(true)
				.secure(false)
				.path("/api/v1/auth")
				.maxAge(Duration.ofSeconds(maxAgeSeconds))
				.sameSite("Lax")
				.build();
	}

	public static ResponseCookie clearRefreshCookie() {
		return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
				.httpOnly(true)
				.secure(false)
				.path("/api/v1/auth")
				.maxAge(0)
				.sameSite("Lax")
				.build();
	}
}
