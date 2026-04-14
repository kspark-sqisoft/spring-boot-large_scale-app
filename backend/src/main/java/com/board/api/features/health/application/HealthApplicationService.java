package com.board.api.features.health.application;

import org.springframework.stereotype.Service;

import com.board.api.features.health.domain.HealthStatus;

@Service
public class HealthApplicationService implements GetHealthStatusUseCase {

	@Override
	public HealthStatus getStatus() {
		return new HealthStatus("UP", "board-api");
	}
}
