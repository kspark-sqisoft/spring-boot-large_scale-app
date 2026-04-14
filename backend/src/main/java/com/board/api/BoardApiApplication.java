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

@SpringBootApplication(exclude = {
		RedisAutoConfiguration.class,
		RedisRepositoriesAutoConfiguration.class,
		KafkaAutoConfiguration.class
})
@EnableConfigurationProperties({
		JwtProperties.class,
		SecurityBootstrapProperties.class,
		CorsProperties.class,
		FileStorageProperties.class
})
public class BoardApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(BoardApiApplication.class, args);
	}

}
