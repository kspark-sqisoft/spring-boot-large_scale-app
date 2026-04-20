package com.board.api.features.health.application;

import org.springframework.stereotype.Service;

import com.board.api.features.health.domain.HealthStatus;

// 단순 구현: 프로세스가 살아 있으면 UP (DB 핑 등은 필요 시 확장)
@Service
public class HealthApplicationService implements GetHealthStatusUseCase {

	@Override
	public HealthStatus getStatus() {
		return new HealthStatus("UP", "board-api");
	}
}
