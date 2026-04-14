package com.board.api.features.comment.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.board.api.features.comment.domain.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	List<Comment> findByPostIdOrderByCreatedAtAsc(long postId);
}
