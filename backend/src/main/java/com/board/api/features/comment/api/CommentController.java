package com.board.api.features.comment.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.board.api.common.security.AppUserDetails;
import com.board.api.features.comment.api.dto.CommentListResponse;
import com.board.api.features.comment.api.dto.CommentResponse;
import com.board.api.features.comment.api.dto.CreateCommentRequest;
import com.board.api.features.comment.api.dto.UpdateCommentRequest;
import com.board.api.features.comment.application.CommentCommandService;
import com.board.api.features.comment.application.CommentQueryService;

/** 특정 게시글 하위 댓글: 목록은 공개, 쓰기·수정·삭제는 로그인 필요 */
@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
public class CommentController {

	private final CommentCommandService commentCommandService;
	private final CommentQueryService commentQueryService;

	public CommentController(CommentCommandService commentCommandService, CommentQueryService commentQueryService) {
		this.commentCommandService = commentCommandService;
		this.commentQueryService = commentQueryService;
	}

	@GetMapping
	public CommentListResponse list(@PathVariable long postId) {
		return commentQueryService.listForPost(postId);
	}

	@PostMapping
	@PreAuthorize("isAuthenticated()")
	@ResponseStatus(HttpStatus.CREATED)
	public CommentResponse create(
			@AuthenticationPrincipal AppUserDetails principal,
			@PathVariable long postId,
			@Valid @RequestBody CreateCommentRequest request) {
		return commentCommandService.create(postId, principal.getUserId(), request);
	}

	@PutMapping("/{commentId}")
	@PreAuthorize("isAuthenticated()")
	public CommentResponse update(
			@AuthenticationPrincipal AppUserDetails principal,
			@PathVariable long postId,
			@PathVariable long commentId,
			@Valid @RequestBody UpdateCommentRequest request) {
		return commentCommandService.update(postId, commentId, principal.getUserId(), request);
	}

	@DeleteMapping("/{commentId}")
	@PreAuthorize("isAuthenticated()")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(
			@AuthenticationPrincipal AppUserDetails principal,
			@PathVariable long postId,
			@PathVariable long commentId) {
		commentCommandService.delete(postId, commentId, principal.getUserId());
	}
}
