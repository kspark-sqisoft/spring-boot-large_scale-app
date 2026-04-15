package com.board.api.features.auth.api;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.board.api.common.exception.ApiException;
import com.board.api.common.security.JwtTokenProvider;
import com.board.api.features.auth.api.dto.AccessTokenResponse;
import com.board.api.features.auth.api.dto.AuthSessionResponse;
import com.board.api.features.auth.api.dto.LoginRequest;
import com.board.api.features.auth.api.dto.RegisterRequest;
import com.board.api.features.auth.application.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/** 회원가입·로그인·토큰 갱신·로그아웃. 리프레시는 HttpOnly 쿠키, 액세스는 응답 본문(Bearer). */
@RestController
@RequestMapping(AuthApiPaths.BASE)
public class AuthController {

	private final AuthService authService;
	private final JwtTokenProvider jwtTokenProvider;

	public AuthController(AuthService authService, JwtTokenProvider jwtTokenProvider) {
		this.authService = authService;
		this.jwtTokenProvider = jwtTokenProvider;
	}

	@PostMapping("/register")
	public ResponseEntity<AuthSessionResponse> register(@Valid @RequestBody RegisterRequest request) {
		var issue = authService.register(request.email(), request.password());
		return sessionResponse(issue, HttpStatus.CREATED);
	}

	@PostMapping("/login")
	public ResponseEntity<AuthSessionResponse> login(@Valid @RequestBody LoginRequest request) {
		var issue = authService.login(request.email(), request.password());
		return sessionResponse(issue, HttpStatus.OK);
	}

	/** 쿠키의 리프레시로 새 액세스 발급, Set-Cookie로 리프레시 로테이션 */
	@PostMapping("/refresh")
	public ResponseEntity<AccessTokenResponse> refresh(HttpServletRequest httpRequest) {
		String raw = AuthCookie.readRefreshRaw(httpRequest)
				.orElseThrow(() -> new ApiException(
						HttpStatus.UNAUTHORIZED,
						"MISSING_REFRESH",
						"리프레시 쿠키가 없습니다."));
		var issue = authService.refresh(raw);
		return ResponseEntity.ok()
				.header(
						HttpHeaders.SET_COOKIE,
						AuthCookie.refreshCookie(issue.rawRefreshToken(), jwtTokenProvider).toString())
				.body(AccessTokenResponse.of(issue.accessToken(), issue.expiresInSeconds()));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
		AuthCookie.readRefreshRaw(httpRequest).ifPresent(authService::logout);
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, AuthCookie.clearRefreshCookie().toString())
				.build();
	}

	private ResponseEntity<AuthSessionResponse> sessionResponse(
			com.board.api.features.auth.application.SessionIssue issue,
			HttpStatus status) {
		return ResponseEntity.status(status)
				.header(
						HttpHeaders.SET_COOKIE,
						AuthCookie.refreshCookie(issue.rawRefreshToken(), jwtTokenProvider).toString())
				.body(AuthSessionResponse.from(issue));
	}
}
