package com.board.api.common.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.board.api.features.auth.domain.User;
import com.board.api.features.auth.domain.UserRole;

public class AppUserDetails implements UserDetails {

	private final Long userId;
	private final String email;
	private final String passwordHash;
	private final UserRole role;

	public AppUserDetails(Long userId, String email, String passwordHash, UserRole role) {
		this.userId = userId;
		this.email = email;
		this.passwordHash = passwordHash;
		this.role = role;
	}

	public static AppUserDetails fromUser(User user) {
		return new AppUserDetails(user.getId(), user.getEmail(), user.getPasswordHash(), user.getRole());
	}

	public Long getUserId() {
		return userId;
	}

	public UserRole getRole() {
		return role;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
