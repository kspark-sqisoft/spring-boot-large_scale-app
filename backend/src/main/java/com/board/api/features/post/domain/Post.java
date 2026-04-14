package com.board.api.features.post.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "posts")
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

	protected Post() {
	}

	public Post(Long id, Long authorUserId, String title, String content, Instant createdAt, Instant updatedAt) {
		this.id = id;
		this.authorUserId = authorUserId;
		this.title = title;
		this.content = content;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public static Post create(Long id, String title, String content) {
		return create(id, null, title, content);
	}

	public static Post create(Long id, Long authorUserId, String title, String content) {
		Instant now = Instant.now();
		return new Post(id, authorUserId, title, content, now, now);
	}

	public Long getId() {
		return id;
	}

	public Long getAuthorUserId() {
		return authorUserId;
	}

	public String getTitle() {
		return title;
	}

	public String getContent() {
		return content;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
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
