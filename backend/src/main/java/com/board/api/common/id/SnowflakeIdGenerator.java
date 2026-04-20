package com.board.api.common.id;

import org.springframework.stereotype.Component;

/**
 * 단일 JVM·개발 환경용 Snowflake 스타일 64비트 ID (datacenter/worker 고정).
 * 멀티 인스턴스·클럭 스큐 시 운영 규칙을 별도로 두어야 합니다.
 *
 * [Snowflake ID 구조]
 * 64비트 = (현재시각 - 기준시각) 42비트 | 데이터센터ID 5비트 | 워커ID 5비트 | 시퀀스 12비트
 * 덕분에 분산 환경에서도 충돌 없이 시간 순서대로 유일한 ID를 생성할 수 있다.
 */
// @Component: Spring이 이 클래스를 Bean으로 등록해 DI(의존성 주입)에 쓸 수 있게 함
@Component
public class SnowflakeIdGenerator {

	// 기준 시각: 2021-01-01 00:00:00 UTC (이 값을 빼서 42비트 안에 들어오게 함)
	private static final long EPOCH_MS = 1_609_459_200_000L;

	// 데이터센터 ID를 비트 위치 17번으로 올리기 위한 시프트 값
	private static final long DATACENTER_SHIFT = 17L;

	// 워커 ID를 비트 위치 12번으로 올리기 위한 시프트 값
	private static final long WORKER_SHIFT = 12L;

	// 시퀀스 마스크: 하위 12비트만 남김 (0~4095 범위, 같은 밀리초에 최대 4096개 ID 발급 가능)
	private static final long SEQUENCE_MASK = 0xFFFL;

	private final long datacenterId; // 데이터센터 식별자 (0~31)
	private final long workerId;     // 워커(서버 인스턴스) 식별자 (0~31)

	private long sequence = 0L;          // 같은 밀리초 내 순번
	private long lastTimestamp = -1L;    // 마지막으로 ID를 발급한 시각(ms)

	// 기본 생성자: 개발 환경에서는 datacenter=1, worker=1 고정 사용
	public SnowflakeIdGenerator() {
		this(1L, 1L);
	}

	// 테스트·다중 인스턴스 배포 시 직접 ID 값을 지정할 수 있는 생성자
	SnowflakeIdGenerator(long datacenterId, long workerId) {
		// datacenterId/workerId 가 5비트 범위(0~31)를 벗어나면 오류
		if ((datacenterId & ~0x1FL) != 0 || (workerId & ~0x1FL) != 0) {
			throw new IllegalArgumentException("datacenterId and workerId must be 0..31");
		}
		this.datacenterId = datacenterId;
		this.workerId = workerId;
	}

	// synchronized: 멀티스레드 환경에서 동시에 호출돼도 ID가 중복되지 않도록 잠금
	public synchronized long nextId() {
		long timestamp = System.currentTimeMillis(); // 현재 시각(ms)

		// 시스템 클럭이 뒤로 가면(NTP 보정 등) ID 중복 위험 → 예외 발생
		if (timestamp < lastTimestamp) {
			throw new IllegalStateException("Clock moved backwards; refusing to generate id");
		}

		if (timestamp == lastTimestamp) {
			// 같은 밀리초 안에 또 호출됨 → 시퀀스 증가
			sequence = (sequence + 1) & SEQUENCE_MASK;
			if (sequence == 0) {
				// 시퀀스가 4095를 넘으면(4096번째 호출) 다음 밀리초가 될 때까지 대기
				timestamp = waitNextMillis(lastTimestamp);
			}
		}
		else {
			// 새 밀리초 → 시퀀스 초기화
			sequence = 0L;
		}

		lastTimestamp = timestamp; // 마지막 발급 시각 갱신

		// 비트 OR 연산으로 64비트 ID 조립
		// [타임스탬프 42비트] | [데이터센터 5비트] | [워커 5비트] | [시퀀스 12비트]
		return ((timestamp - EPOCH_MS) << 22)
				| ((datacenterId & 0x1FL) << DATACENTER_SHIFT)
				| ((workerId & 0x1FL) << WORKER_SHIFT)
				| sequence;
	}

	// 현재 밀리초가 last보다 커질 때까지 바쁜 대기(busy-wait)
	private static long waitNextMillis(long last) {
		long ts = System.currentTimeMillis();
		while (ts <= last) {
			ts = System.currentTimeMillis();
		}
		return ts;
	}
}
