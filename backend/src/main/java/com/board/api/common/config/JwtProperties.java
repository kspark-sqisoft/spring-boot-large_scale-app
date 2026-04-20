package com.board.api.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * application.yml의 app.jwt.* 설정을 바인딩하는 클래스.
 * @ConfigurationProperties: 지정한 prefix로 시작하는 yml 설정을 필드에 자동 주입.
 */
@ConfigurationProperties(prefix = "app.jwt")
@Getter  // Lombok: 모든 필드의 getter 자동 생성
@Setter  // Lombok: 모든 필드의 setter 자동 생성 (yml 바인딩 시 필요)
public class JwtProperties {

	/**
	 * JWT 서명에 쓸 비밀 문자열 (최소 32바이트 권장).
	 * 운영 환경에서는 반드시 환경변수(APP_JWT_SECRET)로 주입해야 합니다.
	 * 기본값은 개발용 더미 — 절대 운영에 사용하지 마세요.
	 */
	private String secret = "dev-only-change-this-secret-32chars-min!!";

	// 액세스 토큰 유효 시간 (분 단위, 기본값 15분)
	// 짧게 설정해야 탈취 시 피해 최소화
	private long accessExpirationMinutes = 15;

	// 리프레시 토큰 유효 기간 (일 단위, 기본값 7일)
	// 액세스 토큰이 만료되면 이 토큰으로 새 액세스 토큰을 발급
	private long refreshExpirationDays = 7;
}
