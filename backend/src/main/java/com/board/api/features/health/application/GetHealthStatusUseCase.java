package com.board.api.features.health.application;

import com.board.api.features.health.domain.HealthStatus;

/** 헬스 컨트롤러가 호출하는 응용 유스케이스(구현체에서 DB·Redis 등 핑) */
// 인터페이스로 두면 테스트에서 가짜 구현으로 교체하기 쉬움
public interface GetHealthStatusUseCase {

	HealthStatus getStatus();
}
