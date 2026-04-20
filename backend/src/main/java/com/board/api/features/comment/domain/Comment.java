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

/**
 * 댓글 도메인 엔티티.
 * DB의 "post_comments" 테이블과 매핑됩니다.
 *
 * 댓글 구조: 최대 2단계
 * - 루트 댓글 (parentId == null): 게시글에 직접 달리는 댓글
 * - 대댓글 (parentId != null): 루트 댓글에 달리는 답글 (대댓글에는 다시 답글 불가)
 */
@Entity
@Table(name = "post_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 내부용 기본 생성자
@AllArgsConstructor                                 // 정적 팩토리에서 사용
public class Comment {

	@Id
	private Long id; // Snowflake ID

	@Column(name = "post_id", nullable = false)
	private Long postId; // 이 댓글이 달린 게시글 ID

	@Column(name = "parent_id")
	private Long parentId; // 대댓글이면 부모 댓글 ID, 루트 댓글이면 null

	@Column(name = "author_user_id", nullable = false)
	private Long authorUserId; // 댓글 작성자 사용자 ID

	@Column(nullable = false, length = 2000)
	private String content; // 댓글 내용 (최대 2,000자)

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt; // 작성 시각

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt; // 수정 시각

	// 루트 댓글(1단계) 생성: parentId를 null로 설정
	public static Comment createRoot(long id, long postId, long authorUserId, String content) {
		Instant now = Instant.now();
		return new Comment(id, postId, null, authorUserId, content, now, now);
	}

	// 대댓글(2단계) 생성: parentId를 부모 댓글 ID로 설정
	public static Comment createReply(long id, long postId, long parentId, long authorUserId, String content) {
		Instant now = Instant.now();
		return new Comment(id, postId, parentId, authorUserId, content, now, now);
	}

	// 댓글 내용 수정
	public void setContent(String content) {
		this.content = content;
	}

	// 수정 시각 갱신
	public void touchUpdatedAt() {
		this.updatedAt = Instant.now();
	}

	// 루트 댓글 여부 (true: 1단계, false: 2단계 대댓글)
	public boolean isRoot() {
		return parentId == null;
	}

	// 댓글 깊이 반환 (0: 루트, 1: 대댓글)
	public int depth() {
		return parentId == null ? 0 : 1;
	}
}
