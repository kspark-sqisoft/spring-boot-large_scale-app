package com.board.api.common.exception;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

/** API 오류 공통 JSON 형태(code·message·timestamp) */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
		String code,
		String message,
		Instant timestamp
) {

	public static ErrorResponse of(String code, String message) {
		return new ErrorResponse(code, message, Instant.now());
	}
}
