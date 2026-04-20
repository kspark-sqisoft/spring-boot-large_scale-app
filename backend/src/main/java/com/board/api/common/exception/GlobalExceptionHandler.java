package com.board.api.common.exception;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

/**
 * 컨트롤러 전역 예외 핸들러.
 * 모든 컨트롤러에서 발생하는 예외를 한 곳에서 잡아 일관된 JSON {@link ErrorResponse}로 반환합니다.
 */
// @Slf4j: Lombok이 log 변수를 자동 생성 → log.error(...), log.warn(...) 으로 로깅
@Slf4j
// @RestControllerAdvice: 모든 @RestController에 AOP로 적용되는 예외 처리 클래스
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * @Valid 검증 실패 시 처리 (예: 이메일 형식 오류, 필수 필드 누락).
	 * HTTP 400 Bad Request로 응답하며 어떤 필드가 왜 잘못됐는지 메시지에 담아줌.
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		// getFieldErrors(): 어떤 필드가 잘못됐는지 목록
		// f.getField(): 필드명 (예: "email"), f.getDefaultMessage(): 오류 이유 (예: "이메일 형식이어야 합니다")
		// reduce: 여러 오류를 "; "로 이어 붙여 하나의 문자열로 만듦
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(f -> f.getField() + ": " + Objects.requireNonNullElse(f.getDefaultMessage(), ""))
				.reduce((a, b) -> a + "; " + b)
				.orElse("Validation failed");
		return ResponseEntity
				.badRequest()                                               // 400 상태코드
				.body(ErrorResponse.of("VALIDATION_ERROR", message));       // JSON 바디
	}

	/**
	 * 서비스 레이어에서 의도적으로 던진 ApiException 처리.
	 * 예: 게시글 없음(404), 권한 없음(403), 이미 사용 중인 이메일(409) 등
	 */
	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
		return ResponseEntity
				.status(Objects.requireNonNull(ex.getStatus()))              // ApiException에 담긴 HTTP 상태코드
				.body(ErrorResponse.of(ex.getCode(), Objects.requireNonNullElse(ex.getMessage(), "")));
	}

	/**
	 * 예상치 못한 예외(버그, 외부 시스템 장애 등) → 500 Internal Server Error.
	 * 상세 내용은 로그에만 남기고 클라이언트에는 최소 정보만 노출.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
		log.error("Unhandled error", ex); // 서버 로그에 스택 트레이스 기록
		return ResponseEntity
				.status(HttpStatus.INTERNAL_SERVER_ERROR)                     // 500
				.body(ErrorResponse.of("INTERNAL_ERROR", "서버 오류가 발생했습니다."));
	}
}
