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

// @Slf4j: 로깅
@Slf4j
@Service
@RequiredArgsConstructor
public class PostLikeCommandService {

	private final PostRepository postRepository;
	private final PostLikeRepository postLikeRepository;
	private final SnowflakeIdGenerator idGenerator;

	// 좋아요 추가: 멱등에 가깝게 — 이미 있으면 DB INSERT 없이 현재 상태만 반환
	@Transactional
	public PostLikeStatusResponse like(long postId, long userId) {
		ensurePostExists(postId);
		if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
			return readStatus(postId, userId);
		}
		try {
			// saveAndFlush: 즉시 INSERT를 DB에 보내 유니크 제약 위반을 이 자리에서 잡기 위함
			postLikeRepository.saveAndFlush(new PostLike(
					idGenerator.nextId(),
					postId,
					userId,
					Instant.now()));
		}
		catch (DataIntegrityViolationException e) {
			if (isDuplicateLikeRow(e)) {
				// 동시 요청 등으로 (post_id, user_id) 유니크 충돌 → 이미 좋아요된 것과 동일하게 취급
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

	// 좋아요 취소: row가 없어도 delete는 조용히 0건일 수 있음 → 항상 최신 count 반환
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

	// API 응답용: 총 좋아요 수 + 내가 눌렀는지 여부
	private PostLikeStatusResponse readStatus(long postId, long userId) {
		long count = postLikeRepository.countByPostId(postId);
		boolean liked = postLikeRepository.existsByPostIdAndUserId(postId, userId);
		return new PostLikeStatusResponse(count, liked);
	}

	// DB 벤더마다 에러 메시지 문자열이 달라서 키워드로 대략 판별
	private static boolean isDuplicateLikeRow(DataIntegrityViolationException e) {
		String msg = String.valueOf(e.getMostSpecificCause().getMessage());
		String lower = msg.toLowerCase();
		return lower.contains("duplicate")
				|| lower.contains("unique")
				|| lower.contains("uk_post_likes_post_user");
	}
}
