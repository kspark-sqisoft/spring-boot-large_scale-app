package com.board.api.features.health.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.board.api.features.health.api.dto.HealthResponse;
import com.board.api.features.health.application.GetHealthStatusUseCase;
import lombok.RequiredArgsConstructor;

/** DB·Redis·Kafka 등 부속 연결 상태를 한 번에 조회(모니터링·학습용) */
@RestController
@RequestMapping(HealthApiPaths.BASE)
@RequiredArgsConstructor
public class HealthController {

	// 유스케이스(애플리케이션 서비스): 컨트롤러는 HTTP만, 상태 판별은 여기에 위임
	private final GetHealthStatusUseCase getHealthStatusUseCase;

	// GET /api/v1/health — 로드밸런서·쿠버네티스 프로브에서도 자주 쓰는 패턴
	@GetMapping(HealthApiPaths.HEALTH)
	public HealthResponse health() {
		return HealthResponse.from(getHealthStatusUseCase.getStatus());
	}
}
