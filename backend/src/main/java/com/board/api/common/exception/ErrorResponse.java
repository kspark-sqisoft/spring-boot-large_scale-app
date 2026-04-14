package com.board.api.common.exception;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

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
