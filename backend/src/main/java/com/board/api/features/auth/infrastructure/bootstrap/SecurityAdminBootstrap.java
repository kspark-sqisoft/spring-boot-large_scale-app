package com.board.api.features.auth.infrastructure.bootstrap;

import java.util.Objects;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.board.api.common.config.SecurityBootstrapProperties;
import com.board.api.common.id.SnowflakeIdGenerator;
import com.board.api.features.auth.domain.User;
import com.board.api.features.auth.domain.UserRole;
import com.board.api.features.auth.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;

/** 개발 편의: 설정된 최초 관리자 계정이 없으면 기동 시 한 번 생성 */
// ApplicationRunner: 애플리케이션 기동 완료 후 run() 한 번 실행
@Component
@Order(100)
@ConditionalOnProperty(name = "app.security.bootstrap-admin", havingValue = "true")
@RequiredArgsConstructor
public class SecurityAdminBootstrap implements ApplicationRunner {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final SnowflakeIdGenerator idGenerator;
	private final SecurityBootstrapProperties properties;

	@Override
	public void run(ApplicationArguments args) {
		String email = Objects.requireNonNull(properties.getInitialAdminEmail(), "initialAdminEmail")
				.trim()
				.toLowerCase();
		if (userRepository.existsByEmail(email)) {
			// 이미 계정이 있으면 아무 것도 하지 않음(멱등)
			return;
		}
		String rawPassword = Objects.requireNonNull(properties.getInitialAdminPassword(), "initialAdminPassword");
		User admin = User.create(
				idGenerator.nextId(),
				email,
				passwordEncoder.encode(rawPassword),
				UserRole.ADMIN);
		userRepository.save(admin);
	}
}
