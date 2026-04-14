package com.board.api.features.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.board.api.common.exception.ApiException;
import com.board.api.common.id.SnowflakeIdGenerator;
import com.board.api.common.security.JwtTokenProvider;
import com.board.api.features.auth.domain.RefreshToken;
import com.board.api.features.auth.domain.User;
import com.board.api.features.auth.domain.UserRole;
import com.board.api.features.auth.infrastructure.persistence.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private AuthenticationManager authenticationManager;
	@Mock
	private JwtTokenProvider jwtTokenProvider;
	@Mock
	private RefreshTokenService refreshTokenService;
	@Mock
	private SnowflakeIdGenerator idGenerator;

	@InjectMocks
	private AuthService authService;

	private User existingUser;

	@BeforeEach
	void setUp() {
		existingUser = User.create(42L, "user@example.com", "stored-hash", UserRole.USER);
	}

	@Test
	void register_normalizes_email_and_hashes_password() {
		when(userRepository.existsByEmail("mixed@example.com")).thenReturn(false);
		when(idGenerator.nextId()).thenReturn(100L);
		when(passwordEncoder.encode("secretpass")).thenReturn("encoded");
		when(jwtTokenProvider.createAccessToken(any(User.class))).thenReturn("access.jwt");
		when(jwtTokenProvider.getAccessExpirationSeconds()).thenReturn(900L);
		when(refreshTokenService.issue(anyLong(), anyString())).thenAnswer(inv -> {
			long uid = inv.getArgument(0);
			String raw = inv.getArgument(1);
			return new RefreshToken(1L, uid, TokenHasher.sha256Hex(raw), null, null);
		});

		SessionIssue session = authService.register("  Mixed@Example.COM ", "secretpass");

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		User saved = userCaptor.getValue();
		assertThat(saved.getEmail()).isEqualTo("mixed@example.com");
		assertThat(saved.getPasswordHash()).isEqualTo("encoded");
		assertThat(saved.getRole()).isEqualTo(UserRole.USER);
		assertThat(session.accessToken()).isEqualTo("access.jwt");
		verify(refreshTokenService).issue(eq(100L), anyString());
	}

	@Test
	void register_duplicate_email_conflict() {
		when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);
		assertThatThrownBy(() -> authService.register("taken@example.com", "pw"))
				.isInstanceOf(ApiException.class)
				.extracting(ex -> ((ApiException) ex).getStatus())
				.isEqualTo(HttpStatus.CONFLICT);
		verify(userRepository, never()).save(any());
	}

	@Test
	void login_bad_credentials_maps_to_api_exception() {
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenThrow(new BadCredentialsException("bad"));
		assertThatThrownBy(() -> authService.login("u@example.com", "wrong"))
				.isInstanceOf(ApiException.class)
				.extracting(ex -> ((ApiException) ex).getStatus())
				.isEqualTo(HttpStatus.UNAUTHORIZED);
		verify(jwtTokenProvider, never()).createAccessToken(any());
	}

	@Test
	void refresh_revokes_old_and_issues_new_session() {
		RefreshToken stored = new RefreshToken(
				5L,
				42L,
				"hash",
				java.time.Instant.now().plusSeconds(3600),
				java.time.Instant.now());
		when(refreshTokenService.findValid("raw-refresh")).thenReturn(Optional.of(stored));
		when(userRepository.findById(42L)).thenReturn(Optional.of(existingUser));
		when(jwtTokenProvider.createAccessToken(existingUser)).thenReturn("new.access");
		when(jwtTokenProvider.getAccessExpirationSeconds()).thenReturn(60L);
		when(refreshTokenService.issue(eq(42L), anyString())).thenAnswer(inv -> {
			String raw = inv.getArgument(1);
			return new RefreshToken(6L, 42L, TokenHasher.sha256Hex(raw), null, null);
		});

		SessionIssue out = authService.refresh("raw-refresh");

		verify(refreshTokenService).revoke(stored);
		assertThat(out.accessToken()).isEqualTo("new.access");
		verify(refreshTokenService).issue(eq(42L), anyString());
	}

	@Test
	void logout_revokes_when_raw_present() {
		authService.logout("some-raw");
		verify(refreshTokenService).revokeByRawToken("some-raw");
	}

	@Test
	void logout_noop_when_blank() {
		authService.logout("  ");
		verify(refreshTokenService, never()).revokeByRawToken(anyString());
	}
}
