package com.board.api.features.post.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글 좋아요 도메인 엔티티.
 * DB의 "post_likes" 테이블과 매핑됩니다.
 *
 * 한 사용자가 같은 게시글에 좋아요를 중복으로 누를 수 없도록
 * (post_id, user_id) 조합에 유니크 제약 조건이 걸려 있습니다.
 */
@Entity
// @Table: 유니크 제약 조건 — 같은 (post_id, user_id) 쌍이 두 번 들어오면 DB 에러
@Table(
		name = "post_likes",
		uniqueConstraints = @UniqueConstraint(name = "uk_post_likes_post_user", columnNames = { "post_id", "user_id" }))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 내부용 기본 생성자
@AllArgsConstructor                                 // PostLikeCommandService에서 new PostLike(...) 생성 시 사용
public class PostLike {

	@Id
	private Long id; // Snowflake ID

	@Column(name = "post_id", nullable = false)
	private Long postId; // 좋아요를 누른 게시글 ID

	@Column(name = "user_id", nullable = false)
	private Long userId; // 좋아요를 누른 사용자 ID

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt; // 좋아요를 누른 시각
}
