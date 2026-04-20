package com.board.api.common.exception;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

/** 컨트롤러 전역 예외 → 일관된 JSON {@link ErrorResponse} */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(f -> f.getField() + ": " + Objects.requireNonNullElse(f.getDefaultMessage(), ""))
				.reduce((a, b) -> a + "; " + b)
				.orElse("Validation failed");
		return ResponseEntity
				.badRequest()
				.body(ErrorResponse.of("VALIDATION_ERROR", message));
	}

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
		return ResponseEntity
				.status(Objects.requireNonNull(ex.getStatus()))
				.body(ErrorResponse.of(ex.getCode(), Objects.requireNonNullElse(ex.getMessage(), "")));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
		log.error("Unhandled error", ex);
		return ResponseEntity
				.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ErrorResponse.of("INTERNAL_ERROR", "서버 오류가 발생했습니다."));
	}
}
