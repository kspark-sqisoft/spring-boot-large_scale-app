package com.board.api.features.health.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.board.api.features.health.api.dto.HealthResponse;
import com.board.api.features.health.application.GetHealthStatusUseCase;

@RestController
@RequestMapping(HealthApiPaths.BASE)
public class HealthController {

	private final GetHealthStatusUseCase getHealthStatusUseCase;

	public HealthController(GetHealthStatusUseCase getHealthStatusUseCase) {
		this.getHealthStatusUseCase = getHealthStatusUseCase;
	}

	@GetMapping(HealthApiPaths.HEALTH)
	public HealthResponse health() {
		return HealthResponse.from(getHealthStatusUseCase.getStatus());
	}
}
