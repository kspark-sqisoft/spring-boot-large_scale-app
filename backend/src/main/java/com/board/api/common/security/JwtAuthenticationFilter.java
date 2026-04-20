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
import lombok.RequiredArgsConstructor;

/**
 * 요청마다 {@code Authorization: Bearer <token>} 액세스 JWT를 검증하는 필터.
 * 성공 시 {@link SecurityContextHolder}에 사용자 정보를 저장해 컨트롤러에서
 * {@code @AuthenticationPrincipal}로 꺼낼 수 있게 합니다.
 *
 * OncePerRequestFilter: 한 HTTP 요청당 딱 한 번만 실행되도록 보장하는 Spring 기반 필터
 */
// @Component: Spring Bean 등록 → SecurityConfig에서 필터 체인에 추가됨
// @RequiredArgsConstructor: final 필드를 인자로 받는 생성자를 Lombok이 자동 생성
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider jwtTokenProvider; // JWT 파싱·검증 담당
	private final ObjectMapper objectMapper;          // 에러 응답을 JSON으로 직렬화

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {

		// 1. Authorization 헤더 확인
		String header = request.getHeader(HttpHeaders.AUTHORIZATION); // "Bearer eyJhbGci..."
		if (header == null || !header.startsWith("Bearer ")) {
			// 헤더가 없거나 Bearer 형식이 아님 → 익명 사용자로 다음 필터로 통과
			// 공개 API(게시글 조회 등)는 SecurityConfig에서 permitAll이라 문제없음
			filterChain.doFilter(request, response);
			return;
		}

		// 2. "Bearer " 접두사(7글자)를 제거해 순수 토큰 문자열 추출
		String token = header.substring(7).trim();
		if (token.isEmpty()) {
			filterChain.doFilter(request, response); // 빈 토큰 → 통과
			return;
		}

		try {
			// 3. 토큰 검증 및 클레임(페이로드) 파싱
			Claims claims = jwtTokenProvider.parseAccessToken(token);

			// 4. 클레임에서 사용자 정보 추출
			long userId = Long.parseLong(claims.getSubject()); // sub 클레임 = 사용자 ID
			String email = claims.get("email", String.class);   // 이메일
			var role = jwtTokenProvider.parseRole(claims);       // 역할(USER/ADMIN)

			// 5. Spring Security용 Principal 객체 생성 (DB 조회 없이 토큰 클레임만 사용)
			var principal = new AppUserDetails(userId, email, "", role);

			// 6. SecurityContext에 인증 정보 등록
			//    - credentials: null (JWT라 비밀번호 불필요)
			//    - authorities: ROLE_USER 또는 ROLE_ADMIN
			var authentication = new UsernamePasswordAuthenticationToken(
					principal,
					null,
					principal.getAuthorities());
			// 요청 IP·세션 등 부가 정보를 인증 객체에 추가
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			// SecurityContext에 저장 → 이후 컨트롤러에서 @AuthenticationPrincipal로 꺼낼 수 있음
			SecurityContextHolder.getContext().setAuthentication(authentication);
		}
		catch (JwtException | IllegalArgumentException ex) {
			// 토큰 깨짐·만료·서명 불일치 등 → 즉시 401 JSON 응답 (체인 진행 안 함)
			SecurityContextHolder.clearContext(); // 혹시 이전에 인증된 정보가 있으면 지움
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
			response.setCharacterEncoding(StandardCharsets.UTF_8.name());
			response.setContentType(MediaType.APPLICATION_JSON_VALUE); // Content-Type: application/json
			objectMapper.writeValue(
					response.getOutputStream(),
					ErrorResponse.of("INVALID_TOKEN", "액세스 토큰이 유효하지 않습니다."));
			return;
		}

		// 7. 인증 성공 → 다음 필터로 요청 전달
		filterChain.doFilter(request, response);
	}
}
