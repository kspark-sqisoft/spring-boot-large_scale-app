package com.board.api.features.auth.api;

import java.util.Objects;

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
import lombok.RequiredArgsConstructor;

/** 회원가입·로그인·토큰 갱신·로그아웃. 리프레시는 HttpOnly 쿠키, 액세스는 응답 본문(Bearer). */
// @RestController: JSON API 전용 컨트롤러
// @RequestMapping(AuthApiPaths.BASE): /api/v1/auth 등 공통 prefix (AuthApiPaths 참고)
@RestController
@RequestMapping(AuthApiPaths.BASE)
@RequiredArgsConstructor
public class AuthController {

	// 실제 비즈니스 로직(회원 DB 저장, 비밀번호 검증, 리프레시 로테이션 등)
	private final AuthService authService;
	// 쿠키 만료(Max-Age)·속성 설정에 JWT 설정값이 필요
	private final JwtTokenProvider jwtTokenProvider;

	// POST /api/v1/auth/register — 신규 계정 생성 후 바로 세션(액세스+리프레시) 발급
	@PostMapping("/register")
	public ResponseEntity<AuthSessionResponse> register(@Valid @RequestBody RegisterRequest request) {
		// SessionIssue: 액세스 토큰 문자열, 만료초, 리프레시 원시값 등을 담은 불변 객체
		var issue = authService.register(request.email(), request.password());
		// 201 Created + Set-Cookie(리프레시) + JSON(액세스 등)
		return sessionResponse(issue, HttpStatus.CREATED);
	}

	// POST /api/v1/auth/login — 이메일/비밀번호 검증 후 세션 발급
	@PostMapping("/login")
	public ResponseEntity<AuthSessionResponse> login(@Valid @RequestBody LoginRequest request) {
		var issue = authService.login(request.email(), request.password());
		return sessionResponse(issue, HttpStatus.OK);
	}

	/** 쿠키의 리프레시로 새 액세스 발급, Set-Cookie로 리프레시 로테이션 */
	// POST /api/v1/auth/refresh — HttpOnly 쿠키의 리프레시 토큰으로 새 액세스 JWT 발급
	@PostMapping("/refresh")
	public ResponseEntity<AccessTokenResponse> refresh(HttpServletRequest httpRequest) {
		// 요청 헤더 Cookie에서 리프레시 값 파싱 (클라이언트 JS로 읽을 수 없게 HttpOnly 권장)
		String raw = AuthCookie.readRefreshRaw(httpRequest)
				.orElseThrow(() -> new ApiException(
						HttpStatus.UNAUTHORIZED,
						"MISSING_REFRESH",
						"리프레시 쿠키가 없습니다."));
		// DB에서 해시 매칭·만료 검사 후 기존 리프레시 폐기 + 새 쌍 발급
		var issue = authService.refresh(raw);
		return ResponseEntity.ok()
				.header(
						// Set-Cookie: 브라우저가 새 리프레시 쿠키로 교체 저장
						HttpHeaders.SET_COOKIE,
						AuthCookie.refreshCookie(
										Objects.requireNonNull(issue.rawRefreshToken(), "rawRefreshToken"),
										jwtTokenProvider)
								.toString())
				// 액세스는 Authorization 헤더에 넣기 위해 응답 JSON으로 내려줌 (쿠키에 넣지 않는 패턴)
				.body(AccessTokenResponse.of(
						Objects.requireNonNull(issue.accessToken(), "accessToken"),
						issue.expiresInSeconds()));
	}

	// POST /api/v1/auth/logout — 서버(DB)에서 리프레시 무효화 + 브라우저 쿠키 삭제
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
		// 쿠키가 있을 때만 revoke (없어도 204로 성공 처리)
		AuthCookie.readRefreshRaw(httpRequest).ifPresent(authService::logout);
		return ResponseEntity.noContent()
				// Max-Age=0 같은 삭제용 Set-Cookie
				.header(HttpHeaders.SET_COOKIE, AuthCookie.clearRefreshCookie().toString())
				.build();
	}

	// register/login 공통: 상태코드만 다르고 헤더·바디 구성은 동일해서 private로 추출
	private ResponseEntity<AuthSessionResponse> sessionResponse(
			com.board.api.features.auth.application.SessionIssue issue,
			HttpStatus status) {
		return ResponseEntity.status(Objects.requireNonNull(status, "status"))
				.header(
						HttpHeaders.SET_COOKIE,
						AuthCookie.refreshCookie(
										Objects.requireNonNull(issue.rawRefreshToken(), "rawRefreshToken"),
										jwtTokenProvider)
								.toString())
				.body(AuthSessionResponse.from(Objects.requireNonNull(issue, "issue")));
	}
}
