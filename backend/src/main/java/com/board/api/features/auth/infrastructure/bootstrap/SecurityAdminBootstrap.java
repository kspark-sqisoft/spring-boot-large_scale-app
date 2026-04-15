package com.board.api.features.auth.infrastructure.bootstrap;

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

/** 개발 편의: 설정된 최초 관리자 계정이 없으면 기동 시 한 번 생성 */
@Component
@Order(100)
@ConditionalOnProperty(name = "app.security.bootstrap-admin", havingValue = "true")
public class SecurityAdminBootstrap implements ApplicationRunner {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final SnowflakeIdGenerator idGenerator;
	private final SecurityBootstrapProperties properties;

	public SecurityAdminBootstrap(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			SnowflakeIdGenerator idGenerator,
			SecurityBootstrapProperties properties) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.idGenerator = idGenerator;
		this.properties = properties;
	}

	@Override
	public void run(ApplicationArguments args) {
		String email = properties.getInitialAdminEmail().trim().toLowerCase();
		if (userRepository.existsByEmail(email)) {
			return;
		}
		User admin = User.create(
				idGenerator.nextId(),
				email,
				passwordEncoder.encode(properties.getInitialAdminPassword()),
				UserRole.ADMIN);
		userRepository.save(admin);
	}
}
