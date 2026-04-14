package com.board.api.features.post.infrastructure.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.board.api.features.post.domain.PostLike;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

	long countByPostId(long postId);

	boolean existsByPostIdAndUserId(long postId, long userId);

	void deleteByPostIdAndUserId(long postId, long userId);

	@Query("select pl.postId, count(pl) from PostLike pl where pl.postId in :ids group by pl.postId")
	List<Object[]> countGroupedByPostId(@Param("ids") Collection<Long> ids);

	@Query("select pl.postId from PostLike pl where pl.userId = :userId and pl.postId in :ids")
	List<Long> findPostIdsLikedByUser(@Param("userId") long userId, @Param("ids") Collection<Long> ids);
}
