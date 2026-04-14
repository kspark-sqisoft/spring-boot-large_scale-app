package com.board.api.features.post.infrastructure.persistence;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.board.api.features.post.domain.Post;

public interface PostRepository extends JpaRepository<Post, Long> {

	Page<Post> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

	@Query("""
			select p from Post p
			where (p.createdAt < :ca) or (p.createdAt = :ca and p.id < :pid)
			order by p.createdAt desc, p.id desc
			""")
	List<Post> findOlderThan(@Param("ca") Instant createdAt, @Param("pid") long postId, Pageable pageable);
}
