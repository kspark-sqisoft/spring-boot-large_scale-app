package com.board.api.common.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.board.api.common.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 인증되지 않은 사용자가 보호된 리소스에 접근할 때(401 Unauthorized) JSON으로 응답하는 핸들러.
 *
 * 예: 토큰 없이 댓글 작성 API 호출 시 여기서 처리됨.
 * AuthenticationEntryPoint: Spring Security 표준 인터페이스.
 */
// @Component: Spring Bean 등록 → SecurityConfig에서 authenticationEntryPoint로 등록됨
// @RequiredArgsConstructor: final 필드 생성자 자동 생성
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper; // Java 객체 → JSON 변환기

	@Override
	public void commence(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException authException) throws IOException {

		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);           // 401 상태코드
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());       // 한글 깨짐 방지
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);          // Content-Type: application/json

		// 응답 바디: { "code": "UNAUTHORIZED", "message": "인증이 필요합니다.", "timestamp": "..." }
		ErrorResponse body = ErrorResponse.of("UNAUTHORIZED", "인증이 필요합니다.");
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
