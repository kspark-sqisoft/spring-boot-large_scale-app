package com.board.api.features.health.api.dto;

import com.board.api.features.health.domain.HealthStatus;

public record HealthResponse(
		String status,
		String service
) {

	public static HealthResponse from(HealthStatus status) {
		return new HealthResponse(status.status(), status.service());
	}
}
