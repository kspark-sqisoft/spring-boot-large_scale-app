package com.board.api.common.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.board.api.features.auth.domain.User;
import com.board.api.features.auth.domain.UserRole;
import lombok.RequiredArgsConstructor;

/**
 * Spring Security가 인증된 사용자를 나타낼 때 쓰는 Principal 객체.
 * 컨트롤러에서 {@code @AuthenticationPrincipal AppUserDetails principal}로 꺼낼 수 있습니다.
 *
 * UserDetails: Spring Security 표준 인터페이스. 이를 구현하면 Security 인프라와 자연스럽게 연동됨.
 */
// @RequiredArgsConstructor: final 필드를 인자로 받는 생성자 자동 생성
@RequiredArgsConstructor
public class AppUserDetails implements UserDetails {

	private final Long userId;        // DB의 사용자 ID (Snowflake)
	private final String email;       // 로그인 이메일
	private final String passwordHash; // 비밀번호 해시 (DB 로그인 시 사용, JWT 방식에서는 빈 문자열)
	private final UserRole role;      // 사용자 역할 (USER / ADMIN)

	// DB에서 조회한 User 엔티티로 AppUserDetails 생성하는 정적 팩토리
	public static AppUserDetails fromUser(User user) {
		return new AppUserDetails(user.getId(), user.getEmail(), user.getPasswordHash(), user.getRole());
	}

	// 컨트롤러에서 현재 로그인 사용자 ID를 꺼낼 때 사용
	public Long getUserId() {
		return userId;
	}

	// @PreAuthorize나 비즈니스 로직에서 역할 확인에 사용
	public UserRole getRole() {
		return role;
	}

	/**
	 * Spring Security가 권한 목록을 확인할 때 호출.
	 * "ROLE_USER" 또는 "ROLE_ADMIN" 형태로 반환해야 @PreAuthorize("hasRole('ADMIN')")이 작동함.
	 */
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// SimpleGrantedAuthority: 단순 문자열 권한 ("ROLE_USER", "ROLE_ADMIN")
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	// Spring Security가 비밀번호 검증 시 사용 (JWT 인증에선 직접 쓰이지 않음)
	@Override
	public String getPassword() {
		return passwordHash;
	}

	// Spring Security에서 '사용자명' = 이메일로 취급
	@Override
	public String getUsername() {
		return email;
	}

	// 계정 만료 여부 (현재는 만료 기능 미구현 → 항상 false)
	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	// 계정 잠금 여부 (현재는 잠금 기능 미구현 → 항상 잠기지 않음)
	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	// 자격증명(비밀번호) 만료 여부 (현재는 미구현 → 항상 유효)
	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	// 계정 활성화 여부 (현재는 항상 활성)
	@Override
	public boolean isEnabled() {
		return true;
	}
}
