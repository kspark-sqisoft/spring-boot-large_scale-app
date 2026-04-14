package com.board.api.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(RedisViewProperties.class)
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
public class RedisPostViewConfiguration {

	@Bean
	public LettuceConnectionFactory redisConnectionFactory(RedisViewProperties properties) {
		RedisStandaloneConfiguration cfg = new RedisStandaloneConfiguration();
		cfg.setHostName(properties.host());
		cfg.setPort(properties.port());
		return new LettuceConnectionFactory(cfg);
	}

	@Bean
	public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
		return new StringRedisTemplate(connectionFactory);
	}
}
