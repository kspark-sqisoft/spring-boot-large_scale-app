package com.board.api.features.post.infrastructure.kafka;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.board.api.common.config.KafkaBoardProperties;
import com.board.api.features.post.application.PostViewEventPublisher;
import com.board.api.features.post.application.event.PostViewedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaPostViewEventPublisher implements PostViewEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(KafkaPostViewEventPublisher.class);

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final KafkaBoardProperties properties;
	private final ObjectMapper objectMapper;

	public KafkaPostViewEventPublisher(
			KafkaTemplate<String, String> kafkaTemplate,
			KafkaBoardProperties properties,
			ObjectMapper objectMapper) {
		this.kafkaTemplate = kafkaTemplate;
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	@Override
	public void publishPostViewed(long postId) {
		try {
			String json = objectMapper.writeValueAsString(new PostViewedEvent(postId, Instant.now()));
			kafkaTemplate.send(properties.topicPostViewed(), Long.toString(postId), json);
		} catch (JsonProcessingException e) {
			log.warn("Failed to serialize post view event postId={}", postId, e);
		} catch (Exception e) {
			log.warn("Failed to publish post view event postId={}", postId, e);
		}
	}
}
