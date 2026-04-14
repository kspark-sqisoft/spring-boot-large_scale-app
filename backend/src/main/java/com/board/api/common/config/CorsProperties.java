package com.board.api.common.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

	/**
	 * 쉼표로 구분된 origin 목록 (예: {@code http://localhost:5173,http://127.0.0.1:5173}).
	 */
	private String allowedOrigins = "http://localhost:5173,http://127.0.0.1:5173";

	public String getAllowedOrigins() {
		return allowedOrigins;
	}

	public void setAllowedOrigins(String allowedOrigins) {
		this.allowedOrigins = allowedOrigins;
	}

	public List<String> originList() {
		return Arrays.stream(allowedOrigins.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.toList();
	}
}
