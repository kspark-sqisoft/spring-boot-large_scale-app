package com.board.api.features.comment.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "post_comments")
public class Comment {

	@Id
	private Long id;

	@Column(name = "post_id", nullable = false)
	private Long postId;

	@Column(name = "parent_id")
	private Long parentId;

	@Column(name = "author_user_id", nullable = false)
	private Long authorUserId;

	@Column(nullable = false, length = 2000)
	private String content;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Comment() {
	}

	public Comment(Long id, Long postId, Long parentId, Long authorUserId, String content, Instant createdAt,
			Instant updatedAt) {
		this.id = id;
		this.postId = postId;
		this.parentId = parentId;
		this.authorUserId = authorUserId;
		this.content = content;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public static Comment createRoot(long id, long postId, long authorUserId, String content) {
		Instant now = Instant.now();
		return new Comment(id, postId, null, authorUserId, content, now, now);
	}

	public static Comment createReply(long id, long postId, long parentId, long authorUserId, String content) {
		Instant now = Instant.now();
		return new Comment(id, postId, parentId, authorUserId, content, now, now);
	}

	public Long getId() {
		return id;
	}

	public Long getPostId() {
		return postId;
	}

	public Long getParentId() {
		return parentId;
	}

	public Long getAuthorUserId() {
		return authorUserId;
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

	public void setContent(String content) {
		this.content = content;
	}

	public void touchUpdatedAt() {
		this.updatedAt = Instant.now();
	}

	public boolean isRoot() {
		return parentId == null;
	}

	public int depth() {
		return parentId == null ? 0 : 1;
	}
}
