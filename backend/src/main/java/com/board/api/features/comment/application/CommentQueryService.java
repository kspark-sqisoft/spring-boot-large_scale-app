package com.board.api.features.comment.application;

import java.util.List;
import java.util.Map;
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

@Service
public class CommentQueryService {

	private final PostRepository postRepository;
	private final CommentRepository commentRepository;
	private final UserRepository userRepository;

	public CommentQueryService(
			PostRepository postRepository,
			CommentRepository commentRepository,
			UserRepository userRepository) {
		this.postRepository = postRepository;
		this.commentRepository = commentRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public CommentListResponse listForPost(long postId) {
		if (!postRepository.existsById(postId)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다.");
		}
		List<Comment> rows = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
		if (rows.isEmpty()) {
			return new CommentListResponse(List.of());
		}
		Set<Long> authorIds = rows.stream().map(Comment::getAuthorUserId).collect(Collectors.toSet());
		Map<Long, User> byId = userRepository.findAllById(authorIds).stream()
				.collect(Collectors.toMap(User::getId, u -> u));
		List<CommentResponse> comments = rows.stream()
				.map(c -> CommentResponse.from(c, toAuthor(byId.get(c.getAuthorUserId()))))
				.toList();
		return new CommentListResponse(comments);
	}

	private static CommentAuthorResponse toAuthor(User user) {
		if (user == null) {
			return new CommentAuthorResponse("0", "(알 수 없음)");
		}
		return new CommentAuthorResponse(Long.toString(user.getId()), UserProfiles.resolveDisplayName(user));
	}
}
