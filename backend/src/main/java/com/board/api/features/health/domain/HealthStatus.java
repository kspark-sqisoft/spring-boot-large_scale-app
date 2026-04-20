package com.board.api.features.health.domain;

/**
 * 헬스 체크 도메인 값. 인프라/런타임 상태를 표현합니다.
 */
// 예: status="UP", service="board-api" — 나중에 DB·Redis 핑 결과를 필드로 늘리기 쉬움
public record HealthStatus(
		String status,
		String service
) {
}
