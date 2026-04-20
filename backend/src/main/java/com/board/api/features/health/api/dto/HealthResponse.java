package com.board.api.features.health.api.dto;

import com.board.api.features.health.domain.HealthStatus;

// HTTP JSON 바디 — 도메인 record를 API 형태로 그대로 노출
public record HealthResponse(
		String status,
		String service
) {

	public static HealthResponse from(HealthStatus status) {
		return new HealthResponse(status.status(), status.service());
	}
}
