package com.board.api.features.post.application.event;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PostViewedEvent(
		@JsonProperty("postId") long postId,
		@JsonProperty("occurredAt") Instant occurredAt
) {
}
