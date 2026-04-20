package com.board.api.features.post.api;

import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import com.board.api.features.post.api.dto.PostCursorPageResponse;
import com.board.api.features.post.api.dto.PostResponse;
import com.board.api.features.post.api.dto.UpdatePostRequest;
import com.board.api.features.post.application.PopularPostsQueryService;
import com.board.api.features.post.application.PostCommandService;
import com.board.api.features.post.application.PostLikeCommandService;
import com.board.api.features.post.application.PostQueryService;
import com.board.api.features.post.application.PostViewEventPublisher;
import com.board.api.features.post.application.PostViewService;
import com.board.api.features.post.domain.Post;
import lombok.RequiredArgsConstructor;

/** 게시글 CRUD·목록(커서)·인기·조회수·좋아요. 조회는 대부분 공개, 쓰기는 인증 필요. */
@RestController
@RequestMapping(PostApiPaths.BASE)
@RequiredArgsConstructor
public class PostController {

	private final PostCommandService postCommandService;
	private final PostQueryService postQueryService;
	private final PostLikeCommandService postLikeCommandService;
	private final PostViewService postViewService;
	private final PostViewEventPublisher postViewEventPublisher;
	private final PopularPostsQueryService popularPostsQueryService;

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

	@GetMapping("/popular")
	public List<PostResponse> popular(
			@RequestParam(defaultValue = "10") int limit,
			Authentication authentication) {
		return popularPostsQueryService.listPopular(limit, viewerId(authentication));
	}

	@GetMapping("/{postId}")
	public ResponseEntity<PostResponse> get(@PathVariable long postId, Authentication authentication) {
		// Redis(옵션) 조회수 증가 + Kafka(옵션) 조회 이벤트
		long viewCount = postViewService.incrementAndGet(postId);
		postViewEventPublisher.publishPostViewed(postId);
		PostResponse body = postQueryService.getDetailWithViewCount(postId, viewerId(authentication), viewCount);
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.body(body);
	}

	@GetMapping
	public ResponseEntity<PostCursorPageResponse> list(
			@RequestParam(required = false) String cursor,
			@RequestParam(defaultValue = "20") int size,
			Authentication authentication) {
		PostCursorPageResponse body = postQueryService.listPostsByCursor(cursor, size, viewerId(authentication));
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.body(body);
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
				principal.getRole(),
				request.title(),
				request.content(),
				imageIdsOrNull);
		return postQueryService.buildResponse(post, principal.getUserId());
	}

	@DeleteMapping("/{postId}")
	@PreAuthorize("isAuthenticated()")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(
			@AuthenticationPrincipal AppUserDetails principal,
			@PathVariable long postId) {
		postCommandService.delete(postId, principal.getUserId(), principal.getRole());
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
