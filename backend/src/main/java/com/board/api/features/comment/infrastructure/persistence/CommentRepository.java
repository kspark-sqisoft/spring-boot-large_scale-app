package com.board.api.features.comment.infrastructure.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.board.api.features.comment.domain.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	List<Comment> findByPostIdOrderByCreatedAtAsc(long postId);

	long countByPostId(long postId);

	// 게시글 여러 개에 대한 댓글 수 집계 — PostQueryService에서 Map으로 변환
	@Query("select c.postId, count(c) from Comment c where c.postId in :ids group by c.postId")
	List<Object[]> countGroupedByPostId(@Param("ids") Collection<Long> ids);
}
