package com.board.api.common.exception;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * API 오류 공통 JSON 형태.
 * 클라이언트에 반환되는 에러 바디: { "code": "...", "message": "...", "timestamp": "..." }
 */
// @JsonInclude(NON_NULL): null인 필드는 JSON 직렬화에서 제외 (timestamp가 null이면 응답에 안 나옴)
@JsonInclude(JsonInclude.Include.NON_NULL)
// record: Java 14+의 불변 데이터 클래스. 생성자·getter·equals·hashCode·toString 자동 생성
public record ErrorResponse(
		String code,      // 에러 코드 (예: "VALIDATION_ERROR")
		String message,   // 사람이 읽을 수 있는 에러 메시지
		Instant timestamp // 에러 발생 시각 (ISO-8601)
) {

	// 정적 팩토리 메서드: code와 message만 넣으면 timestamp를 자동으로 현재 시각으로 채워줌
	public static ErrorResponse of(String code, String message) {
		return new ErrorResponse(code, message, Instant.now());
	}
}
