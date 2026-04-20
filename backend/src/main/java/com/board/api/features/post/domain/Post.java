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

/**
 * 게시글 도메인 엔티티.
 * DB의 "posts" 테이블과 매핑됩니다.
 */
@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 내부용 기본 생성자
@AllArgsConstructor                                 // 모든 필드 생성자 (정적 팩토리에서 사용)
public class Post {

	@Id
	private Long id; // Snowflake ID (애플리케이션에서 생성)

	@Column(name = "author_user_id")
	private Long authorUserId; // 작성자 사용자 ID (null 허용: 익명 테스트 데이터용)

	@Column(nullable = false, length = 500)
	private String title; // 게시글 제목 (최대 500자)

	@Column(nullable = false, length = 10_000)
	private String content; // 게시글 본문 (최대 10,000자)

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt; // 작성 시각 (변경 불가)

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt; // 마지막 수정 시각

	// 익명(작성자 없는) 게시글 생성 - 테스트용
	public static Post create(Long id, String title, String content) {
		return create(id, null, title, content);
	}

	// 실제 작성자가 있는 게시글 생성
	public static Post create(Long id, Long authorUserId, String title, String content) {
		Instant now = Instant.now();
		return new Post(id, authorUserId, title, content, now, now);
	}

	// 제목 수정
	public void setTitle(String title) {
		this.title = title;
	}

	// 본문 수정
	public void setContent(String content) {
		this.content = content;
	}

	// 수정 시각을 현재 시각으로 갱신
	public void touchUpdatedAt() {
		this.updatedAt = Instant.now();
	}
}
