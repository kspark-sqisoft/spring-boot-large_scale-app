package com.board.api.features.post.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글-이미지 연결 도메인 엔티티.
 * DB의 "post_images" 테이블과 매핑됩니다.
 *
 * 게시글 하나에 여러 이미지를 첨부할 수 있으며,
 * 이 테이블이 게시글(post_id)과 파일(file_id)을 N:M으로 연결합니다.
 */
@Entity
@Table(name = "post_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 내부용 기본 생성자
@AllArgsConstructor                                 // PostCommandService에서 new PostImage(...) 생성 시 사용
public class PostImage {

	@Id
	private Long id; // Snowflake ID

	@Column(name = "post_id", nullable = false)
	private Long postId; // 어느 게시글의 이미지인지

	@Column(name = "file_id", nullable = false)
	private Long fileId; // 실제 파일 메타데이터 ID (StoredFile.id 참조)

	@Column(name = "sort_order", nullable = false)
	private int sortOrder; // 이미지 표시 순서 (0부터 시작, 오름차순 정렬)
}
