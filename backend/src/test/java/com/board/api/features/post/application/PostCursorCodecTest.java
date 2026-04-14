package com.board.api.features.post.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class PostCursorCodecTest {

	@Test
	void roundTrip_preserves_submillisecondNanos() {
		Instant t = Instant.parse("2026-04-14T15:58:55.086823Z");
		long postId = 699275493958094877L;
		String enc = PostCursorCodec.encode(t, postId);
		PostCursorCodec.Cursor c = PostCursorCodec.decode(enc);
		assertThat(c.createdAt()).isEqualTo(t);
		assertThat(c.postId()).isEqualTo(postId);
	}

	@Test
	void legacy_millisFormat_stillDecodes() {
		String legacy = java.util.Base64.getUrlEncoder()
				.withoutPadding()
				.encodeToString("1776179469781:699275493958094877".getBytes(java.nio.charset.StandardCharsets.UTF_8));
		PostCursorCodec.Cursor c = PostCursorCodec.decode(legacy);
		assertThat(c.postId()).isEqualTo(699275493958094877L);
		assertThat(c.createdAt()).isEqualTo(Instant.ofEpochMilli(1776179469781L));
	}
}
