package com.board.api.features.auth.infrastructure.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.board.api.common.security.AppUserDetails;
import com.board.api.features.auth.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;

// Spring Security가 로그인 시 호출하는 UserDetailsService 구현체
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	@Override
	// username 파라미터: 이 프로젝트에서는 "이메일"을 사용자명으로 사용
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		return userRepository.findByEmail(username)
				.map(AppUserDetails::fromUser)
				.orElseThrow(() -> new UsernameNotFoundException(username));
	}
}
