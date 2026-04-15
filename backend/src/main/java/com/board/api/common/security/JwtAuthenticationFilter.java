package com.board.api.common.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.board.api.common.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Claims;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 요청마다 {@code Authorization: Bearer} 액세스 JWT를 검증하고, 성공 시 {@link SecurityContextHolder}에 사용자를 넣습니다.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider jwtTokenProvider;
	private final ObjectMapper objectMapper;

	public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, ObjectMapper objectMapper) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {
		// Bearer 없으면 익명 요청으로 통과(공개 API는 SecurityConfig에서 permitAll)
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}
		String token = header.substring(7).trim();
		if (token.isEmpty()) {
			filterChain.doFilter(request, response);
			return;
		}
		try {
			Claims claims = jwtTokenProvider.parseAccessToken(token);
			long userId = Long.parseLong(claims.getSubject());
			String email = claims.get("email", String.class);
			var role = jwtTokenProvider.parseRole(claims);
			var principal = new AppUserDetails(userId, email, "", role);
			var authentication = new UsernamePasswordAuthenticationToken(
					principal,
					null,
					principal.getAuthorities());
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authentication);
		}
		catch (JwtException | IllegalArgumentException ex) {
			// 토큰 깨짐·만료 등 → 401 JSON으로 즉시 응답(체인 진행 안 함)
			SecurityContextHolder.clearContext();
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setCharacterEncoding(StandardCharsets.UTF_8.name());
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			objectMapper.writeValue(
					response.getOutputStream(),
					ErrorResponse.of("INVALID_TOKEN", "액세스 토큰이 유효하지 않습니다."));
			return;
		}
		filterChain.doFilter(request, response);
	}
}
