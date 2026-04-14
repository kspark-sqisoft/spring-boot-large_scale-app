package com.board.api.features.post.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.board.api.common.security.AppUserDetails;
import com.board.api.features.post.api.dto.CreatePostRequest;
import com.board.api.features.post.api.dto.PostLikeStatusResponse;
import com.board.api.features.post.api.dto.PostPageResponse;
import com.board.api.features.post.api.dto.PostResponse;
import com.board.api.features.post.api.dto.UpdatePostRequest;
import com.board.api.features.post.application.PostCommandService;
import com.board.api.features.post.application.PostLikeCommandService;
import com.board.api.features.post.application.PostQueryService;
import com.board.api.features.post.domain.Post;

@RestController
@RequestMapping(PostApiPaths.BASE)
public class PostController {

	private final PostCommandService postCommandService;
	private final PostQueryService postQueryService;
	private final PostLikeCommandService postLikeCommandService;

	public PostController(
			PostCommandService postCommandService,
			PostQueryService postQueryService,
			PostLikeCommandService postLikeCommandService) {
		this.postCommandService = postCommandService;
		this.postQueryService = postQueryService;
		this.postLikeCommandService = postLikeCommandService;
	}

	@PostMapping
	@PreAuthorize("isAuthenticated()")
	@ResponseStatus(HttpStatus.CREATED)
	public PostResponse create(
			@AuthenticationPrincipal AppUserDetails principal,
			@Valid @RequestBody CreatePostRequest request) {
		List<Long> imageIds = parseLongIds(request.imageFileIds());
		Post post = postCommandService.create(principal.getUserId(), request.title(), request.content(), imageIds);
		return postQueryService.buildResponse(post, principal.getUserId());
	}

	@GetMapping("/{postId}")
	public PostResponse get(@PathVariable long postId, Authentication authentication) {
		return postQueryService.getDetail(postId, viewerId(authentication));
	}

	@GetMapping
	public PostPageResponse list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			Authentication authentication) {
		return postQueryService.listPosts(page, size, viewerId(authentication));
	}

	@PutMapping("/{postId}")
	@PreAuthorize("isAuthenticated()")
	public PostResponse update(
			@AuthenticationPrincipal AppUserDetails principal,
			@PathVariable long postId,
			@Valid @RequestBody UpdatePostRequest request) {
		List<Long> imageIdsOrNull = request.imageFileIds() == null
				? null
				: parseLongIds(request.imageFileIds());
		Post post = postCommandService.update(
				postId,
				principal.getUserId(),
				request.title(),
				request.content(),
				imageIdsOrNull);
		return postQueryService.buildResponse(post, principal.getUserId());
	}

	@DeleteMapping("/{postId}")
	@PreAuthorize("isAuthenticated()")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable long postId) {
		postCommandService.delete(postId);
	}

	@PostMapping("/{postId}/likes")
	@PreAuthorize("isAuthenticated()")
	public PostLikeStatusResponse addLike(
			@AuthenticationPrincipal AppUserDetails principal,
			@PathVariable long postId) {
		return postLikeCommandService.like(postId, principal.getUserId());
	}

	@DeleteMapping("/{postId}/likes")
	@PreAuthorize("isAuthenticated()")
	public PostLikeStatusResponse removeLike(
			@AuthenticationPrincipal AppUserDetails principal,
			@PathVariable long postId) {
		return postLikeCommandService.unlike(postId, principal.getUserId());
	}

	private static Long viewerId(Authentication authentication) {
		if (authentication == null
				|| !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return null;
		}
		Object p = authentication.getPrincipal();
		if (p instanceof AppUserDetails details) {
			return details.getUserId();
		}
		return null;
	}

	private static List<Long> parseLongIds(List<String> ids) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}
		return ids.stream().map(String::trim).filter(s -> !s.isEmpty()).map(Long::parseLong).toList();
	}
}
