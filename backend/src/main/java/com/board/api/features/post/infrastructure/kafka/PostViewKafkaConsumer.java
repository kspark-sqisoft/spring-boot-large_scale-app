package com.board.api.features.post.infrastructure.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.board.api.features.post.application.PopularPostsScoreWriter;
import com.board.api.features.post.application.event.PostViewedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
public class PostViewKafkaConsumer {

	private final ObjectMapper objectMapper;
	private final PopularPostsScoreWriter popularPostsScoreWriter;

	@KafkaListener(
			topics = "${app.kafka.topic-post-viewed:board.post.viewed}",
			containerFactory = "kafkaListenerContainerFactory")
	public void onPostViewed(String payload) {
		try {
			PostViewedEvent event = objectMapper.readValue(payload, PostViewedEvent.class);
			popularPostsScoreWriter.incrementScore(event.postId());
		} catch (Exception e) {
			log.warn("Failed to handle post viewed message: {}", payload, e);
		}
	}
}
