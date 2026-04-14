package com.board.api.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka")
public record KafkaBoardProperties(
		String bootstrapServers,
		String topicPostViewed,
		String consumerGroup
) {
	public KafkaBoardProperties {
		if (bootstrapServers == null || bootstrapServers.isBlank()) {
			bootstrapServers = "localhost:9092";
		}
		if (topicPostViewed == null || topicPostViewed.isBlank()) {
			topicPostViewed = "board.post.viewed";
		}
		if (consumerGroup == null || consumerGroup.isBlank()) {
			consumerGroup = "board-api-popular";
		}
	}
}
