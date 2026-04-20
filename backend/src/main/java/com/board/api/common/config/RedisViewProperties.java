package com.board.api.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 app.redis.view.* 설정을 바인딩하는 record.
 * 조회수 카운터·인기글 점수 저장에 사용하는 Redis 연결 정보를 관리합니다.
 */
@ConfigurationProperties(prefix = "app.redis.view")
public record RedisViewProperties(
		String host, // Redis 서버 호스트 (예: "localhost" 또는 Docker 서비스명 "redis")
		int port     // Redis 포트 (기본: 6379)
) {
	// Compact 생성자: null/빈 값에 기본값 적용
	public RedisViewProperties {
		if (host == null || host.isBlank()) {
			host = "localhost"; // 기본 호스트
		}
		if (port <= 0) {
			port = 6379; // Redis 기본 포트
		}
	}
}
