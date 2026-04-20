package com.board.api.features.comment.application;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.board.api.common.exception.ApiException;
import com.board.api.features.auth.application.UserProfiles;
import com.board.api.features.auth.domain.User;
import com.board.api.features.auth.infrastructure.persistence.UserRepository;
import com.board.api.features.comment.api.dto.CommentAuthorResponse;
import com.board.api.features.comment.api.dto.CommentListResponse;
import com.board.api.features.comment.api.dto.CommentResponse;
import com.board.api.features.comment.domain.Comment;
import com.board.api.features.comment.infrastructure.persistence.CommentRepository;
import com.board.api.features.post.infrastructure.persistence.PostRepository;
import lombok.RequiredArgsConstructor;

/** 게시글별 댓글 트리·작성자 요약 조회 */
@Service
@RequiredArgsConstructor
public class CommentQueryService {

	private final PostRepository postRepository;
	private final CommentRepository commentRepository;
	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public CommentListResponse listForPost(long postId) {
		if (!postRepository.existsById(postId)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다.");
		}
		// 작성순 정렬 — 프론트에서 parentId 기준으로 트리 UI 구성 가능
		List<Comment> rows = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
		if (rows.isEmpty()) {
			return new CommentListResponse(List.of());
		}
		// 댓글 N개에 대한 작성자 정보를 한 번에 조회 (N+1 방지)
		Set<Long> authorIds = rows.stream()
				.map(Comment::getAuthorUserId)
				.map(id -> Objects.requireNonNull(id, "authorUserId"))
				.collect(Collectors.toSet());
		Map<Long, User> byId = userRepository.findAllById(authorIds).stream()
				.collect(Collectors.toMap(User::getId, u -> u));
		List<CommentResponse> comments = rows.stream()
				.map(c -> CommentResponse.from(c, toAuthor(byId.get(c.getAuthorUserId()))))
				.toList();
		return new CommentListResponse(comments);
	}

	// 사용자 row가 없으면(데이터 불일치) 안전한 플레이스홀더
	private static CommentAuthorResponse toAuthor(User user) {
		if (user == null) {
			return new CommentAuthorResponse("0", "(알 수 없음)");
		}
		return new CommentAuthorResponse(Long.toString(user.getId()), UserProfiles.resolveDisplayName(user));
	}
}
