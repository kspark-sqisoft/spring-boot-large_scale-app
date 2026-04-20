package com.board.api.features.post.application;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import org.springframework.http.HttpStatus;

import com.board.api.common.exception.ApiException;

/**
 * 목록 커서: {@code createdAt} + {@code postId} 를 URL-safe Base64로 인코딩합니다.
 * <p>
 * 형식(현재): {@code epochSecond:nanosOfSecond:postId} — DB·{@link Instant} 나노초 정밀도를 유지해
 * {@code createdAt = :cursorInstant} 비교 시 키셋 페이지가 비지 않도록 합니다.
 * <p>
 * 이전 형식 {@code epochMillis:postId} 도 디코드만 호환합니다.
 */
// 유틸 클래스: 컨트롤러가 아닌 순수 함수 모음 — Spring Bean 아님
public final class PostCursorCodec {

	private PostCursorCodec() {
	}

	public static String encode(Instant createdAt, long postId) {
		// 나노초까지 포함해야 동일 시각에 여러 글이 있어도 키셋 페이지가 비지 않음
		String raw = createdAt.getEpochSecond() + ":" + createdAt.getNano() + ":" + postId;
		// URL 쿼리에 넣기 쉬운 Base64url (패딩 없음)
		return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
	}

	public static Cursor decode(String encoded) {
		if (encoded == null || encoded.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CURSOR", "커서 값이 올바르지 않습니다.");
		}
		try {
			byte[] bytes = Base64.getUrlDecoder().decode(encoded);
			String raw = new String(bytes, StandardCharsets.UTF_8);
			String[] parts = raw.split(":", -1);
			if (parts.length == 3) {
				long sec = Long.parseLong(parts[0]);
				int nano = Integer.parseInt(parts[1]);
				long id = Long.parseLong(parts[2]);
				return new Cursor(Instant.ofEpochSecond(sec, nano), id);
			}
			if (parts.length == 2) {
				// 구버전 호환: 밀리초만 있던 커서
				long millis = Long.parseLong(parts[0]);
				long id = Long.parseLong(parts[1]);
				return new Cursor(Instant.ofEpochMilli(millis), id);
			}
			throw new IllegalArgumentException("bad shape");
		}
		catch (Exception e) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CURSOR", "커서 값이 올바르지 않습니다.");
		}
	}

	// 커서 한 덩어리: "이 시각·이 id보다 더 옛날 글" 경계
	public record Cursor(Instant createdAt, long postId) {
	}
}
