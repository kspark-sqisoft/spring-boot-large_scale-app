package com.board.api.features.auth.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.board.api.features.auth.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {

	// 로그인·프로필에서 이메일(정규화된 소문자)로 사용자 조회
	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);
}
