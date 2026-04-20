package com.board.api.features.post.infrastructure.persistence;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.board.api.features.post.domain.Post;

// JpaRepository<Entity, Id>: CRUD + 페이징·정렬 메서드 이름 규칙으로 쿼리 자동 생성
public interface PostRepository extends JpaRepository<Post, Long> {

	// 메서드 이름 → "ORDER BY createdAt DESC, id DESC" 쿼리 생성 (첫 페이지 목록)
	Page<Post> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

	// 커서 다음 페이지: (createdAt, id)가 커서보다 "더 과거"인 행만 — 키셋 페이지네이션
	@Query("""
			select p from Post p
			where (p.createdAt < :ca) or (p.createdAt = :ca and p.id < :pid)
			order by p.createdAt desc, p.id desc
			""")
	List<Post> findOlderThan(@Param("ca") Instant createdAt, @Param("pid") long postId, Pageable pageable);
}
