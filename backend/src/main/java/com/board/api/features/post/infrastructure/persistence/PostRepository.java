package com.board.api.features.post.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.board.api.features.post.domain.Post;

public interface PostRepository extends JpaRepository<Post, Long> {

	Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
