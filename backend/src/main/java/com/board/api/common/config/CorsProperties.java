package com.board.api.common.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * application.yml의 app.cors.* 설정을 바인딩하는 클래스.
 * 브라우저의 CORS 정책에서 허용할 오리진 목록을 관리합니다.
 */
// prefix="app.cors" → yml의 app.cors.allowed-origins 가 Java 필드 allowedOrigins에 매핑됨
@ConfigurationProperties(prefix = "app.cors")
@Getter
@Setter
public class CorsProperties {

	/**
	 * 쉼표로 구분된 origin 목록.
	 * 예: {@code http://localhost:5173,http://127.0.0.1:5173}
	 * 환경변수 APP_CORS_ALLOWED_ORIGINS로 운영 도메인을 주입하세요.
	 */
	private String allowedOrigins = "http://localhost:5173,http://127.0.0.1:5173";

	/**
	 * 쉼표 구분 문자열을 List<String>으로 변환해 반환.
	 * SecurityConfig의 CORS 설정에서 사용됨.
	 */
	public List<String> originList() {
		return Arrays.stream(allowedOrigins.split(","))
				.map(String::trim)           // 각 항목 앞뒤 공백 제거
				.filter(s -> !s.isEmpty())   // 빈 문자열 제거
				.toList();
	}
}
