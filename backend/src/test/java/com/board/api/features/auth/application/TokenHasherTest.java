package com.board.api.features.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;

class TokenHasherTest {

	@Test
	void sha256Hex_matches_jdk_digest() throws Exception {
		String raw = "refresh-token-raw-value";
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
		String expected = HexFormat.of().formatHex(digest);
		assertThat(TokenHasher.sha256Hex(raw)).isEqualTo(expected);
	}

	@Test
	void sha256Hex_is_deterministic_and_lowercase_hex() {
		String h = TokenHasher.sha256Hex("board");
		assertThat(h).hasSize(64).matches("[0-9a-f]+");
		assertThat(TokenHasher.sha256Hex("board")).isEqualTo(h);
	}
}
