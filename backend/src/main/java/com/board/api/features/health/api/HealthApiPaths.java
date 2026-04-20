package com.board.api.features.health.api;

/** 헬스 체크 URL 조각({@code /api/v1/health}) */
public final class HealthApiPaths {

	public static final String BASE = "/api/v1";
	// HealthController에서 @GetMapping(BASE의 일부로 조합) — 전체 경로는 /api/v1/health
	public static final String HEALTH = "/health";

	private HealthApiPaths() {
	}
}
