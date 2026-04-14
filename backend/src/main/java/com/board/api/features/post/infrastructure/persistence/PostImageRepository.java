package com.board.api.features.post.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.board.api.features.post.domain.PostImage;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

	List<PostImage> findByPostIdOrderBySortOrderAsc(long postId);

	void deleteByPostId(long postId);
}
