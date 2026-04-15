package com.board.api.features.comment.application;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.board.api.common.exception.ApiException;
import com.board.api.common.id.SnowflakeIdGenerator;
import com.board.api.features.auth.application.UserProfiles;
import com.board.api.features.auth.domain.User;
import com.board.api.features.auth.infrastructure.persistence.UserRepository;
import com.board.api.features.comment.api.dto.CommentAuthorResponse;
import com.board.api.features.comment.api.dto.CommentResponse;
import com.board.api.features.comment.api.dto.CreateCommentRequest;
import com.board.api.features.comment.api.dto.UpdateCommentRequest;
import com.board.api.features.comment.domain.Comment;
import com.board.api.features.comment.infrastructure.persistence.CommentRepository;
import com.board.api.features.post.infrastructure.persistence.PostRepository;

/** 댓글 생성·수정·삭제(게시글 존재·작성자 검증 포함) */
@Service
public class CommentCommandService {

	private final PostRepository postRepository;
	private final CommentRepository commentRepository;
	private final UserRepository userRepository;
	private final SnowflakeIdGenerator idGenerator;

	public CommentCommandService(
			PostRepository postRepository,
			CommentRepository commentRepository,
			UserRepository userRepository,
			SnowflakeIdGenerator idGenerator) {
		this.postRepository = postRepository;
		this.commentRepository = commentRepository;
		this.userRepository = userRepository;
		this.idGenerator = idGenerator;
	}

	@Transactional
	public CommentResponse create(long postId, long authorUserId, CreateCommentRequest request) {
		if (!postRepository.existsById(postId)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다.");
		}
		Long parentId = parseOptionalLongId(request.parentCommentId());
		String body = request.content().trim();
		Comment saved;
		if (parentId == null) {
			saved = Comment.createRoot(idGenerator.nextId(), postId, authorUserId, body);
		}
		else {
			Comment parent = commentRepository.findById(parentId)
					.orElseThrow(() -> new ApiException(
							HttpStatus.NOT_FOUND,
							"PARENT_COMMENT_NOT_FOUND",
							"상위 댓글을 찾을 수 없습니다."));
			if (!parent.getPostId().equals(postId)) {
				throw new ApiException(
						HttpStatus.BAD_REQUEST,
						"PARENT_POST_MISMATCH",
						"상위 댓글이 이 게시글에 속하지 않습니다.");
			}
			if (!parent.isRoot()) {
				throw new ApiException(
						HttpStatus.BAD_REQUEST,
						"COMMENT_DEPTH_EXCEEDED",
						"답글에는 다시 답글을 달 수 없습니다. (최대 2단계)");
			}
			saved = Comment.createReply(idGenerator.nextId(), postId, parentId, authorUserId, body);
		}
		commentRepository.save(saved);
		return toResponse(saved);
	}

	@Transactional
	public CommentResponse update(long postId, long commentId, long authorUserId, UpdateCommentRequest request) {
		Comment comment = commentRepository.findById(commentId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다."));
		if (!comment.getPostId().equals(postId)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다.");
		}
		if (!comment.getAuthorUserId().equals(authorUserId)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "COMMENT_FORBIDDEN", "본인 댓글만 수정할 수 있습니다.");
		}
		comment.setContent(request.content().trim());
		comment.touchUpdatedAt();
		commentRepository.save(comment);
		return toResponse(comment);
	}

	@Transactional
	public void delete(long postId, long commentId, long authorUserId) {
		Comment comment = commentRepository.findById(commentId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다."));
		if (!comment.getPostId().equals(postId)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다.");
		}
		if (!comment.getAuthorUserId().equals(authorUserId)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "COMMENT_FORBIDDEN", "본인 댓글만 삭제할 수 있습니다.");
		}
		commentRepository.delete(comment);
	}

	private CommentResponse toResponse(Comment comment) {
		User author = userRepository.findById(comment.getAuthorUserId())
				.orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "AUTHOR_NOT_FOUND", "작성자 정보를 찾을 수 없습니다."));
		CommentAuthorResponse authorDto = new CommentAuthorResponse(
				Long.toString(author.getId()),
				UserProfiles.resolveDisplayName(author));
		return CommentResponse.from(comment, authorDto);
	}

	private static Long parseOptionalLongId(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return Long.parseLong(raw.trim());
		}
		catch (NumberFormatException ex) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PARENT_ID", "상위 댓글 ID가 올바르지 않습니다.");
		}
	}
}
