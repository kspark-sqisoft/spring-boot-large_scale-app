package com.board.api.common.exception;

import org.springframework.http.HttpStatus;

/** 도메인/애플리케이션에서 의도된 오류(HTTP 상태·코드·메시지). {@link GlobalExceptionHandler}가 JSON으로 변환 */
public class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	public ApiException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}
}
