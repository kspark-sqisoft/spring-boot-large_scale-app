package com.board.api.common.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.board.api.common.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 로그인은 됐으나 역할·권한이 부족할 때(403 Forbidden) JSON으로 응답하는 핸들러.
 *
 * 예: 일반 USER가 ADMIN 전용 API에 접근 시 여기서 처리됨.
 * AccessDeniedHandler: Spring Security 표준 인터페이스.
 */
// @Component: Spring Bean 등록 → SecurityConfig에서 accessDeniedHandler로 등록됨
// @RequiredArgsConstructor: final 필드 생성자 자동 생성
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper objectMapper; // Java 객체 → JSON 변환기

	@Override
	public void handle(
			HttpServletRequest request,
			HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {

		response.setStatus(HttpServletResponse.SC_FORBIDDEN);             // 403 상태코드
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());      // 한글 깨짐 방지
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);         // Content-Type: application/json

		// 응답 바디: { "code": "FORBIDDEN", "message": "권한이 없습니다.", "timestamp": "..." }
		ErrorResponse body = ErrorResponse.of("FORBIDDEN", "권한이 없습니다.");
		objectMapper.writeValue(response.getOutputStream(), body);
	}
}
