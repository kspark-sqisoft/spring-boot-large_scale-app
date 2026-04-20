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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostLikeCommandService {

	private final PostRepository postRepository;
	private final PostLikeRepository postLikeRepository;
	private final SnowflakeIdGenerator idGenerator;

	@Transactional
	public PostLikeStatusResponse like(long postId, long userId) {
		ensurePostExists(postId);
		if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
			return readStatus(postId, userId);
		}
		try {
			postLikeRepository.saveAndFlush(new PostLike(
					idGenerator.nextId(),
					postId,
					userId,
					Instant.now()));
		}
		catch (DataIntegrityViolationException e) {
			if (isDuplicateLikeRow(e)) {
				/* 동시 요청 등으로 (post_id, user_id) 유니크 충돌 */
			}
			else {
				log.warn("post like persist failed postId={} userId={}: {}", postId, userId, e.getMostSpecificCause().getMessage());
				throw new ApiException(
						HttpStatus.BAD_REQUEST,
						"LIKE_PERSIST_FAILED",
						"좋아요를 저장할 수 없습니다. 로그인 계정과 게시글이 유효한지 확인해 주세요.");
			}
		}
		return readStatus(postId, userId);
	}

	@Transactional
	public PostLikeStatusResponse unlike(long postId, long userId) {
		ensurePostExists(postId);
		postLikeRepository.deleteByPostIdAndUserId(postId, userId);
		return readStatus(postId, userId);
	}

	private void ensurePostExists(long postId) {
		if (!postRepository.existsById(postId)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다.");
		}
	}

	private PostLikeStatusResponse readStatus(long postId, long userId) {
		long count = postLikeRepository.countByPostId(postId);
		boolean liked = postLikeRepository.existsByPostIdAndUserId(postId, userId);
		return new PostLikeStatusResponse(count, liked);
	}

	private static boolean isDuplicateLikeRow(DataIntegrityViolationException e) {
		String msg = String.valueOf(e.getMostSpecificCause().getMessage());
		String lower = msg.toLowerCase();
		return lower.contains("duplicate")
				|| lower.contains("unique")
				|| lower.contains("uk_post_likes_post_user");
	}
}
