package com.board.api.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * 도메인/애플리케이션에서 의도된 오류(HTTP 상태·코드·메시지).
 * {@link GlobalExceptionHandler}가 JSON으로 변환해 클라이언트에 응답합니다.
 *
 * 사용 예:
 *   throw new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다.");
 */
// @Getter: Lombok이 status, code 필드의 getter 메서드를 자동 생성 (getStatus(), getCode())
@Getter
public class ApiException extends RuntimeException {
	// RuntimeException: 체크 예외(checked exception)가 아니므로 throws 선언 없이 어디서든 던질 수 있음

	private final HttpStatus status; // HTTP 응답 상태코드 (예: 404 NOT_FOUND, 401 UNAUTHORIZED)
	private final String code;       // 클라이언트가 구분할 오류 코드 문자열 (예: "POST_NOT_FOUND")

	public ApiException(HttpStatus status, String code, String message) {
		super(message); // 부모 클래스(RuntimeException)에 메시지 전달 → getMessage()로 꺼낼 수 있음
		this.status = status;
		this.code = code;
	}
}
