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
public final class PostCursorCodec {

	private PostCursorCodec() {
	}

	public static String encode(Instant createdAt, long postId) {
		String raw = createdAt.getEpochSecond() + ":" + createdAt.getNano() + ":" + postId;
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

	public record Cursor(Instant createdAt, long postId) {
	}
}
