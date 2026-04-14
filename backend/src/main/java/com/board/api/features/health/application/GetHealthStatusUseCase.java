package com.board.api.features.health.application;

import com.board.api.features.health.domain.HealthStatus;

public interface GetHealthStatusUseCase {

	HealthStatus getStatus();
}
