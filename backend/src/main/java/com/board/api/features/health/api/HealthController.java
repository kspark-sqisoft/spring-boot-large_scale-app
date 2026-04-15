package com.board.api.features.health.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.board.api.features.health.api.dto.HealthResponse;
import com.board.api.features.health.application.GetHealthStatusUseCase;

/** DB·Redis·Kafka 등 부속 연결 상태를 한 번에 조회(모니터링·학습용) */
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
