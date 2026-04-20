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

/**
 * 액세스 JWT(짧은 만료) 생성·파싱 담당.
 * 리프레시 토큰은 이 클래스 밖에서 원시값+DB 방식으로 관리합니다.
 *
 * JWT(JSON Web Token): 사용자 정보를 서버가 서명한 토큰.
 * 클라이언트가 매 요청 헤더에 담아 보내면, 서버는 서명을 검증해 신원을 확인합니다.
 */
// @Component: Spring Bean으로 등록 → JwtAuthenticationFilter 등에서 주입받아 사용
@Component
public class JwtTokenProvider {

	private final JwtProperties jwtProperties; // application.yml의 JWT 설정 (시크릿, 만료 시간)
	private final SecretKey secretKey;          // HMAC-SHA256 서명에 쓸 비밀 키

	public JwtTokenProvider(JwtProperties jwtProperties) {
		this.jwtProperties = jwtProperties;
		// 설정에 있는 문자열 시크릿을 실제 암호화 키로 변환
		this.secretKey = buildKey(jwtProperties.getSecret());
	}

	/**
	 * 문자열 시크릿을 SHA-256 해시 후 HMAC 키로 변환.
	 * SHA-256 해시로 길이를 32바이트로 고정시켜 JJWT 라이브러리 요구사항을 맞춥니다.
	 */
	private static SecretKey buildKey(String secret) {
		try {
			// SHA-256: 임의 길이 문자열 → 256비트(32바이트) 고정 해시
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
			// Keys.hmacShaKeyFor: 바이트 배열을 HMAC 서명용 키 객체로 변환
			return Keys.hmacShaKeyFor(digest);
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e); // SHA-256은 Java 표준이라 실제로는 발생하지 않음
		}
	}

	/**
	 * 로그인/회원가입 후 클라이언트에 발급할 액세스 토큰 생성.
	 * 페이로드에 userId·email·role을 담아 이후 요청에서 DB 조회 없이 신원 파악 가능.
	 */
	public String createAccessToken(User user) {
		Date now = new Date();                                           // 발급 시각
		long expMs = jwtProperties.getAccessExpirationMinutes() * 60_000L; // 분 → 밀리초
		Date exp = new Date(now.getTime() + expMs);                      // 만료 시각
		return Jwts.builder()
				.subject(Long.toString(user.getId()))                    // sub: 사용자 ID
				.claim("email", user.getEmail())                         // 커스텀 클레임: 이메일
				.claim("role", user.getRole().name())                    // 커스텀 클레임: 역할 (USER/ADMIN)
				.issuedAt(now)                                           // iat: 발급 시각
				.expiration(exp)                                         // exp: 만료 시각
				.signWith(secretKey)                                     // HS256 서명
				.compact();                                              // "header.payload.signature" 문자열 생성
	}

	/**
	 * 요청 헤더에서 꺼낸 토큰을 검증하고 클레임(페이로드) 추출.
	 * 서명 불일치·만료 시 JwtException 발생 → JwtAuthenticationFilter가 잡아 401 응답.
	 */
	public Claims parseAccessToken(String token) {
		return Jwts.parser()
				.verifyWith(secretKey)         // 이 키로 서명 검증
				.build()
				.parseSignedClaims(token)      // 파싱 + 검증 (실패 시 예외)
				.getPayload();                 // 클레임 맵 반환
	}

	// 액세스 토큰 만료까지 남은 시간(초) — 클라이언트가 만료 타이머 설정에 사용
	public long getAccessExpirationSeconds() {
		return jwtProperties.getAccessExpirationMinutes() * 60L;
	}

	// 리프레시 토큰 만료 일수 — 쿠키 Max-Age 계산에 사용
	public long getRefreshExpirationDays() {
		return jwtProperties.getRefreshExpirationDays();
	}

	// 클레임에서 역할(UserRole enum)을 꺼내는 헬퍼
	public UserRole parseRole(Claims claims) {
		return UserRole.valueOf(claims.get("role", String.class));
	}
}
