package com.board.api.features.post.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Post {

	@Id
	private Long id;

	@Column(name = "author_user_id")
	private Long authorUserId;

	@Column(nullable = false, length = 500)
	private String title;

	@Column(nullable = false, length = 10_000)
	private String content;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public static Post create(Long id, String title, String content) {
		return create(id, null, title, content);
	}

	public static Post create(Long id, Long authorUserId, String title, String content) {
		Instant now = Instant.now();
		return new Post(id, authorUserId, title, content, now, now);
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public void touchUpdatedAt() {
		this.updatedAt = Instant.now();
	}
}
