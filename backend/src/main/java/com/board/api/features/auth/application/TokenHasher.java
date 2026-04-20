package com.board.api.features.auth.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

// 리프레시 토큰 원시값은 DB에 넣지 않고 SHA-256 hex로만 저장할 때 사용하는 순수 유틸
public final class TokenHasher {

	private TokenHasher() {
	}

	public static String sha256Hex(String raw) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
			// 소문자 hex 문자열 (DB 컬럼 길이 64와 맞춤)
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
