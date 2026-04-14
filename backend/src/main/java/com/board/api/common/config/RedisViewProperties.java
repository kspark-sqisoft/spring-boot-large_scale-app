package com.board.api.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.redis.view")
public record RedisViewProperties(
		String host,
		int port
) {
	public RedisViewProperties {
		if (host == null || host.isBlank()) {
			host = "localhost";
		}
		if (port <= 0) {
			port = 6379;
		}
	}
}
