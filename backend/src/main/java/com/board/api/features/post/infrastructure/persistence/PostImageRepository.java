package com.board.api.features.post.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.board.api.features.post.domain.PostImage;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

	List<PostImage> findByPostIdOrderBySortOrderAsc(long postId);

	// 게시글 수정 시 이미지 목록 전체 교체 전에 기존 매핑 일괄 삭제
	void deleteByPostId(long postId);
}
