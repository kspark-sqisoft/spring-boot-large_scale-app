package com.board.api.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

	/**
	 * HS256용 비밀(최소 32바이트 권장). 운영에서는 반드시 환경변수로 주입.
	 */
	private String secret = "dev-only-change-this-secret-32chars-min!!";

	private long accessExpirationMinutes = 15;

	private long refreshExpirationDays = 7;

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public long getAccessExpirationMinutes() {
		return accessExpirationMinutes;
	}

	public void setAccessExpirationMinutes(long accessExpirationMinutes) {
		this.accessExpirationMinutes = accessExpirationMinutes;
	}

	public long getRefreshExpirationDays() {
		return refreshExpirationDays;
	}

	public void setRefreshExpirationDays(long refreshExpirationDays) {
		this.refreshExpirationDays = refreshExpirationDays;
	}
}
