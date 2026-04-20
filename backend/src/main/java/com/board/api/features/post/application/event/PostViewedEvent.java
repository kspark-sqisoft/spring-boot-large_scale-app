package com.board.api.features.post.application.event;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

// Kafka JSON 페이로드와 필드명을 맞추기 위한 이벤트 DTO (record = 불변)
public record PostViewedEvent(
		@JsonProperty("postId") long postId,
		@JsonProperty("occurredAt") Instant occurredAt
) {
}
