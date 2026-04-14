package com.board.api.features.post.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "post_likes",
		uniqueConstraints = @UniqueConstraint(name = "uk_post_likes_post_user", columnNames = { "post_id", "user_id" }))
public class PostLike {

	@Id
	private Long id;

	@Column(name = "post_id", nullable = false)
	private Long postId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected PostLike() {
	}

	public PostLike(Long id, Long postId, Long userId, Instant createdAt) {
		this.id = id;
		this.postId = postId;
		this.userId = userId;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public Long getPostId() {
		return postId;
	}

	public Long getUserId() {
		return userId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
