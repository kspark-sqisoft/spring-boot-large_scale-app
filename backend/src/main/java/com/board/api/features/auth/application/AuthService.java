package com.board.api.features.auth.application;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.board.api.common.exception.ApiException;
import com.board.api.common.id.SnowflakeIdGenerator;
import com.board.api.common.security.JwtTokenProvider;
import com.board.api.features.auth.domain.User;
import com.board.api.features.auth.domain.UserRole;
import com.board.api.features.auth.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;

/**
 * 인증 핵심 서비스: 회원가입·로그인·토큰 갱신·로그아웃.
 *
 * 토큰 구조:
 * - 액세스 토큰: JWT, 메모리에 보관 (15분 만료)
 * - 리프레시 토큰: 원시값(rawToken)은 쿠키로, SHA-256 해시만 DB에 저장 (7일 만료)
 */
// @Service: 비즈니스 로직 클래스임을 선언. @Component와 동일하게 Bean으로 등록됨
// @RequiredArgsConstructor: final 필드를 인자로 받는 생성자 자동 생성 → Spring이 의존성 주입
@Service
@RequiredArgsConstructor
public class AuthService {

	// SecureRandom: 암호학적으로 안전한 난수 생성기 (리프레시 토큰 원시값 생성에 사용)
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final UserRepository userRepository;           // 사용자 DB 조회/저장
	private final PasswordEncoder passwordEncoder;          // BCrypt 비밀번호 암호화
	private final AuthenticationManager authenticationManager; // 이메일+비밀번호 인증 처리
	private final JwtTokenProvider jwtTokenProvider;        // JWT 생성
	private final RefreshTokenService refreshTokenService;  // 리프레시 토큰 발급/폐기
	private final SnowflakeIdGenerator idGenerator;         // 유니크 ID 생성

	/**
	 * 회원가입: 이메일 중복 확인 후 사용자 생성 + 세션 발급.
	 * @Transactional: 이 메서드 전체가 하나의 DB 트랜잭션으로 실행됨
	 * (중간에 오류 발생 시 모든 변경 롤백)
	 */
	@Transactional
	public SessionIssue register(String email, String password) {
		// 이메일 정규화: 앞뒤 공백 제거 + 소문자 변환 (대소문자 구분 없이 동일 계정으로 취급)
		String normalized = email.trim().toLowerCase();
		if (userRepository.existsByEmail(normalized)) {
			// 이미 사용 중인 이메일 → 409 Conflict
			throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "이미 사용 중인 이메일입니다.");
		}
		// 비밀번호를 BCrypt 해시로 변환해 저장 (평문 저장 금지!)
		User user = User.create(
				idGenerator.nextId(),
				normalized,
				passwordEncoder.encode(Objects.requireNonNull(password, "password")),
				UserRole.USER); // 신규 가입자는 항상 USER 역할
		userRepository.save(user);
		return issueSession(user); // 가입 직후 바로 로그인 상태로 세션 발급
	}

	/**
	 * 로그인: 이메일+비밀번호 검증 후 세션 발급.
	 * AuthenticationManager가 AppUserDetailsService.loadUserByUsername()을 호출해 검증.
	 */
	@Transactional
	public SessionIssue login(String email, String password) {
		String normalized = email.trim().toLowerCase();
		try {
			// Spring Security의 AuthenticationManager에게 이메일+비밀번호 검증 위임
			// 내부적으로 AppUserDetailsService → UserRepository.findByEmail() → BCrypt 비교
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(
							normalized,
							Objects.requireNonNull(password, "password")));
		}
		catch (BadCredentialsException ex) {
			// 이메일 없음 또는 비밀번호 불일치 → 401 Unauthorized
			// 보안상 어느 쪽이 틀렸는지 구분하지 않음
			throw new ApiException(
					HttpStatus.UNAUTHORIZED,
					"INVALID_CREDENTIALS",
					"이메일 또는 비밀번호가 올바르지 않습니다.");
		}
		// 검증 성공 → DB에서 사용자 조회 (세션 발급에 필요한 전체 정보 로드)
		User user = userRepository.findByEmail(normalized)
				.orElseThrow(() -> new ApiException(
						HttpStatus.UNAUTHORIZED,
						"INVALID_CREDENTIALS",
						"이메일 또는 비밀번호가 올바르지 않습니다."));
		return issueSession(user);
	}

	/**
	 * 토큰 갱신: 기존 리프레시 토큰을 폐기하고 새 액세스+리프레시 쌍 발급.
	 * 토큰 로테이션: 리프레시 토큰도 매 갱신마다 새로 발급해 탈취된 토큰 재사용 방지.
	 */
	@Transactional
	public SessionIssue refresh(String rawRefreshToken) {
		// DB에서 해당 해시값의 유효한(폐기 안 된, 만료 안 된) 리프레시 토큰 조회
		var stored = refreshTokenService.findValid(rawRefreshToken)
				.orElseThrow(() -> new ApiException(
						HttpStatus.UNAUTHORIZED,
						"INVALID_REFRESH",
						"리프레시 토큰이 없거나 만료되었습니다."));
		// 토큰의 userId로 사용자 조회
		User user = userRepository.findById(stored.getUserId())
				.orElseThrow(() -> new ApiException(
						HttpStatus.UNAUTHORIZED,
						"INVALID_REFRESH",
						"사용자를 찾을 수 없습니다."));
		refreshTokenService.revoke(stored); // 기존 토큰 폐기 (로테이션)
		return issueSession(user);          // 새 액세스+리프레시 발급
	}

	/**
	 * 로그아웃: 쿠키의 리프레시 토큰을 DB에서 폐기.
	 * (액세스 토큰은 만료까지 클라이언트가 메모리에서 삭제하는 방식으로 처리)
	 */
	@Transactional
	public void logout(String rawRefreshToken) {
		if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
			refreshTokenService.revokeByRawToken(rawRefreshToken); // 해시 계산 후 DB에서 폐기
		}
	}

	// 액세스 JWT + 리프레시 토큰을 함께 발급하는 내부 메서드
	private SessionIssue issueSession(User user) {
		String accessToken = jwtTokenProvider.createAccessToken(user);        // JWT 생성
		long exp = jwtTokenProvider.getAccessExpirationSeconds();              // 만료까지 남은 초
		String rawRefresh = newRefreshRaw();                                   // 원시 리프레시 토큰 생성
		refreshTokenService.issue(user.getId(), rawRefresh);                   // 해시해서 DB 저장
		return new SessionIssue(user, accessToken, exp, rawRefresh);          // 결과 반환
	}

	// 256비트(32바이트) 무작위 리프레시 토큰 원시값 생성 → URL-safe Base64 인코딩
	private static String newRefreshRaw() {
		byte[] buf = new byte[32]; // 32바이트 = 256비트
		SECURE_RANDOM.nextBytes(buf);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(buf); // 패딩('=') 없는 Base64
	}
}
