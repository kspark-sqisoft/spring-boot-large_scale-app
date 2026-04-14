package com.board.api.features.post.application;

import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.board.api.common.exception.ApiException;
import com.board.api.common.id.SnowflakeIdGenerator;
import com.board.api.features.post.api.dto.PostLikeStatusResponse;
import com.board.api.features.post.domain.PostLike;
import com.board.api.features.post.infrastructure.persistence.PostLikeRepository;
import com.board.api.features.post.infrastructure.persistence.PostRepository;

@Service
public class PostLikeCommandService {

	private final PostRepository postRepository;
	private final PostLikeRepository postLikeRepository;
	private final SnowflakeIdGenerator idGenerator;

	public PostLikeCommandService(
			PostRepository postRepository,
			PostLikeRepository postLikeRepository,
			SnowflakeIdGenerator idGenerator) {
		this.postRepository = postRepository;
		this.postLikeRepository = postLikeRepository;
		this.idGenerator = idGenerator;
	}

	@Transactional
	public PostLikeStatusResponse like(long postId, long userId) {
		ensurePostExists(postId);
		if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
			return status(postId, userId);
		}
		try {
			PostLike row = new PostLike(idGenerator.nextId(), postId, userId, Instant.now());
			postLikeRepository.save(row);
		}
		catch (DataIntegrityViolationException ignored) {
			/* 동시 요청으로 UNIQUE 충돌 시 멱등 처리 */
		}
		return status(postId, userId);
	}

	@Transactional
	public PostLikeStatusResponse unlike(long postId, long userId) {
		ensurePostExists(postId);
		postLikeRepository.deleteByPostIdAndUserId(postId, userId);
		return status(postId, userId);
	}

	private void ensurePostExists(long postId) {
		if (!postRepository.existsById(postId)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다.");
		}
	}

	private PostLikeStatusResponse status(long postId, long userId) {
		long count = postLikeRepository.countByPostId(postId);
		boolean liked = postLikeRepository.existsByPostIdAndUserId(postId, userId);
		return new PostLikeStatusResponse(count, liked);
	}
}
