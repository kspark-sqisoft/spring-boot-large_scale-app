package com.board.api.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 app.kafka.* 설정을 바인딩하는 record.
 * Kafka 브로커 주소·토픽명·컨슈머 그룹 ID를 관리합니다.
 *
 * record: 불변 데이터 클래스. 생성자에서 null/blank 검사 및 기본값을 설정합니다.
 */
@ConfigurationProperties(prefix = "app.kafka")
public record KafkaBoardProperties(
		String bootstrapServers, // Kafka 브로커 주소 (예: "localhost:9092")
		String topicPostViewed,  // 게시글 조회 이벤트를 발행할 토픽명
		String consumerGroup     // 이 앱의 Kafka 컨슈머 그룹 ID
) {
	// Compact 생성자: record 생성 시 자동 호출 → null/빈 값에 기본값 적용
	public KafkaBoardProperties {
		if (bootstrapServers == null || bootstrapServers.isBlank()) {
			bootstrapServers = "localhost:9092"; // 로컬 Kafka 기본 주소
		}
		if (topicPostViewed == null || topicPostViewed.isBlank()) {
			topicPostViewed = "board.post.viewed"; // 조회 이벤트 토픽명
		}
		if (consumerGroup == null || consumerGroup.isBlank()) {
			consumerGroup = "board-api-popular"; // 인기글 점수 업데이트 컨슈머 그룹
		}
	}
}
