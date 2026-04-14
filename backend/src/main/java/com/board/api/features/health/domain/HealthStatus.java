package com.board.api.features.health.domain;

/**
 * 헬스 체크 도메인 값. 인프라/런타임 상태를 표현합니다.
 */
public record HealthStatus(
		String status,
		String service
) {
}
