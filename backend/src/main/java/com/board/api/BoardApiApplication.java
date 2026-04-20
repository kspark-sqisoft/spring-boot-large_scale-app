package com.board.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.board.api.common.config.CorsProperties;
import com.board.api.common.config.FileStorageProperties;
import com.board.api.common.config.JwtProperties;
import com.board.api.common.config.SecurityBootstrapProperties;

/**
 * 게시판 API 진입점. Redis/Kafka 자동설정은 프로필·플래그로 켤 때만 수동 빈으로 구성하므로 여기서 제외합니다.
 */
// @SpringBootApplication: 이 클래스가 Spring Boot 앱의 시작점임을 선언
// exclude: Redis·Kafka 자동 설정을 끔 → app.redis.enabled / app.kafka.enabled 플래그로 필요할 때만 수동 등록
@SpringBootApplication(exclude = {
		RedisAutoConfiguration.class,                // Redis 자동 연결 끔
		RedisRepositoriesAutoConfiguration.class,    // Redis Repository 자동 생성 끔
		KafkaAutoConfiguration.class                 // Kafka 자동 설정 끔
})
// @EnableConfigurationProperties: application.yml의 app.* 설정을 자바 클래스로 바인딩
@EnableConfigurationProperties({
		JwtProperties.class,                  // app.jwt.* 설정 (JWT 시크릿, 만료 시간)
		SecurityBootstrapProperties.class,    // app.security.* 설정 (초기 관리자 계정)
		CorsProperties.class,                 // app.cors.* 설정 (허용 오리진)
		FileStorageProperties.class           // app.upload.* 설정 (파일 저장 경로)
})
public class BoardApiApplication {

	// Spring Boot 앱을 실행하는 메인 메서드
	public static void main(String[] args) {
		SpringApplication.run(BoardApiApplication.class, args);
	}

}
