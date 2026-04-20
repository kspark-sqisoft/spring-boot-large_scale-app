package com.board.api.common.config;

import java.util.Objects;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 연결 Bean을 등록하는 설정 클래스.
 * {@code app.redis.enabled=true}일 때만 활성화됩니다.
 *
 * Redis: 인메모리 키-값 저장소. 조회수 카운터와 인기글 순위(Sorted Set)에 사용.
 * Lettuce: 비동기·논블로킹 Redis 클라이언트 라이브러리.
 */
// @ConditionalOnProperty: app.redis.enabled=true 일 때만 이 설정 클래스 전체 활성화
@Configuration
@EnableConfigurationProperties(RedisViewProperties.class)
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
public class RedisPostViewConfiguration {

	/**
	 * Redis 연결 팩토리.
	 * Lettuce 클라이언트로 단일 Redis 서버(Standalone 모드)에 연결.
	 */
	@Bean
	public LettuceConnectionFactory redisConnectionFactory(RedisViewProperties properties) {
		RedisStandaloneConfiguration cfg = new RedisStandaloneConfiguration();
		cfg.setHostName(Objects.requireNonNull(properties.host(), "host")); // Redis 호스트 설정
		cfg.setPort(properties.port());                                       // Redis 포트 설정
		return new LettuceConnectionFactory(cfg);
	}

	/**
	 * StringRedisTemplate: Redis 연산을 쉽게 쓰는 Spring 래퍼.
	 * 키와 값 모두 String 타입으로 고정 (JSON 직렬화 없이 단순 문자열 저장).
	 * 조회수: opsForValue().increment("board:views:post:123")
	 * 인기글: opsForZSet().incrementScore("board:popular:posts", "123", 1.0)
	 */
	@Bean
	public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
		return new StringRedisTemplate(
				Objects.requireNonNull(connectionFactory, "connectionFactory"));
	}
}
