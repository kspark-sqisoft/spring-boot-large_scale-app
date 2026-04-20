package com.board.api.features.comment.domain;

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
@Table(name = "post_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
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

	public static Comment createRoot(long id, long postId, long authorUserId, String content) {
		Instant now = Instant.now();
		return new Comment(id, postId, null, authorUserId, content, now, now);
	}

	public static Comment createReply(long id, long postId, long parentId, long authorUserId, String content) {
		Instant now = Instant.now();
		return new Comment(id, postId, parentId, authorUserId, content, now, now);
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
