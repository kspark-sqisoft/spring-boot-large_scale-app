package com.board.api.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "app.jwt")
@Getter
@Setter
public class JwtProperties {

	/**
	 * HS256용 비밀(최소 32바이트 권장). 운영에서는 반드시 환경변수로 주입.
	 */
	private String secret = "dev-only-change-this-secret-32chars-min!!";

	private long accessExpirationMinutes = 15;

	private long refreshExpirationDays = 7;
}
