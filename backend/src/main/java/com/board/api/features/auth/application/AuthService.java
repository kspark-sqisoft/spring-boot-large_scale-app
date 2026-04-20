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

/** 회원·비밀번호·세션 발급: 액세스 JWT + DB에 해시 저장된 리프레시 토큰 */
@Service
public class AuthService {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenService refreshTokenService;
	private final SnowflakeIdGenerator idGenerator;

	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager,
			JwtTokenProvider jwtTokenProvider,
			RefreshTokenService refreshTokenService,
			SnowflakeIdGenerator idGenerator) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtTokenProvider = jwtTokenProvider;
		this.refreshTokenService = refreshTokenService;
		this.idGenerator = idGenerator;
	}

	@Transactional
	public SessionIssue register(String email, String password) {
		String normalized = email.trim().toLowerCase();
		if (userRepository.existsByEmail(normalized)) {
			throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "이미 사용 중인 이메일입니다.");
		}
		User user = User.create(
				idGenerator.nextId(),
				normalized,
				passwordEncoder.encode(Objects.requireNonNull(password, "password")),
				UserRole.USER);
		userRepository.save(user);
		return issueSession(user);
	}

	@Transactional
	public SessionIssue login(String email, String password) {
		String normalized = email.trim().toLowerCase();
		try {
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(
							normalized,
							Objects.requireNonNull(password, "password")));
		}
		catch (BadCredentialsException ex) {
			throw new ApiException(
					HttpStatus.UNAUTHORIZED,
					"INVALID_CREDENTIALS",
					"이메일 또는 비밀번호가 올바르지 않습니다.");
		}
		User user = userRepository.findByEmail(normalized)
				.orElseThrow(() -> new ApiException(
						HttpStatus.UNAUTHORIZED,
						"INVALID_CREDENTIALS",
						"이메일 또는 비밀번호가 올바르지 않습니다."));
		return issueSession(user);
	}

	/** 기존 리프레시 폐기 후 새 액세스+리프레시 쌍 발급(로테이션) */
	@Transactional
	public SessionIssue refresh(String rawRefreshToken) {
		var stored = refreshTokenService.findValid(rawRefreshToken)
				.orElseThrow(() -> new ApiException(
						HttpStatus.UNAUTHORIZED,
						"INVALID_REFRESH",
						"리프레시 토큰이 없거나 만료되었습니다."));
		User user = userRepository.findById(stored.getUserId())
				.orElseThrow(() -> new ApiException(
						HttpStatus.UNAUTHORIZED,
						"INVALID_REFRESH",
						"사용자를 찾을 수 없습니다."));
		refreshTokenService.revoke(stored);
		return issueSession(user);
	}

	@Transactional
	public void logout(String rawRefreshToken) {
		if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
			refreshTokenService.revokeByRawToken(rawRefreshToken);
		}
	}

	private SessionIssue issueSession(User user) {
		String accessToken = jwtTokenProvider.createAccessToken(user);
		long exp = jwtTokenProvider.getAccessExpirationSeconds();
		String rawRefresh = newRefreshRaw();
		refreshTokenService.issue(user.getId(), rawRefresh);
		return new SessionIssue(user, accessToken, exp, rawRefresh);
	}

	private static String newRefreshRaw() {
		byte[] buf = new byte[32];
		SECURE_RANDOM.nextBytes(buf);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
	}
}
