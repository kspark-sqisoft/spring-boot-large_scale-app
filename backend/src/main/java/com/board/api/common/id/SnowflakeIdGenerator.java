package com.board.api.common.id;

import org.springframework.stereotype.Component;

/**
 * 단일 JVM·개발 환경용 Snowflake 스타일 64비트 ID (datacenter/worker 고정).
 * 멀티 인스턴스·클럭 스큐 시 운영 규칙을 별도로 두어야 합니다.
 */
@Component
public class SnowflakeIdGenerator {

	private static final long EPOCH_MS = 1_609_459_200_000L; // 2021-01-01 UTC
	private static final long DATACENTER_SHIFT = 17L;
	private static final long WORKER_SHIFT = 12L;
	private static final long SEQUENCE_MASK = 0xFFFL;

	private final long datacenterId;
	private final long workerId;

	private long sequence = 0L;
	private long lastTimestamp = -1L;

	public SnowflakeIdGenerator() {
		this(1L, 1L);
	}

	SnowflakeIdGenerator(long datacenterId, long workerId) {
		if ((datacenterId & ~0x1FL) != 0 || (workerId & ~0x1FL) != 0) {
			throw new IllegalArgumentException("datacenterId and workerId must be 0..31");
		}
		this.datacenterId = datacenterId;
		this.workerId = workerId;
	}

	public synchronized long nextId() {
		long timestamp = System.currentTimeMillis();
		if (timestamp < lastTimestamp) {
			throw new IllegalStateException("Clock moved backwards; refusing to generate id");
		}
		if (timestamp == lastTimestamp) {
			sequence = (sequence + 1) & SEQUENCE_MASK;
			if (sequence == 0) {
				timestamp = waitNextMillis(lastTimestamp);
			}
		}
		else {
			sequence = 0L;
		}
		lastTimestamp = timestamp;
		return ((timestamp - EPOCH_MS) << 22)
				| ((datacenterId & 0x1FL) << DATACENTER_SHIFT)
				| ((workerId & 0x1FL) << WORKER_SHIFT)
				| sequence;
	}

	private static long waitNextMillis(long last) {
		long ts = System.currentTimeMillis();
		while (ts <= last) {
			ts = System.currentTimeMillis();
		}
		return ts;
	}
}
