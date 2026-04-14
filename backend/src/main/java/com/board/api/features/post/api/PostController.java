package com.board.api.features.post.api;

import java.util.List;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.board.api.common.security.AppUserDetails;
import com.board.api.features.post.api.dto.CreatePostRequest;
import com.board.api.features.post.api.dto.PostPageResponse;
import com.board.api.features.post.api.dto.PostResponse;
import com.board.api.features.post.api.dto.UpdatePostRequest;
import com.board.api.features.post.application.PostCommandService;
import com.board.api.features.post.application.PostQueryService;
import com.board.api.features.post.domain.Post;

@RestController
@RequestMapping(PostApiPaths.BASE)
public class PostController {

	private final PostCommandService postCommandService;
	private final PostQueryService postQueryService;

	public PostController(PostCommandService postCommandService, PostQueryService postQueryService) {
		this.postCommandService = postCommandService;
		this.postQueryService = postQueryService;
	}

	@PostMapping
	@PreAuthorize("isAuthenticated()")
	@ResponseStatus(HttpStatus.CREATED)
	public PostResponse create(
			@AuthenticationPrincipal AppUserDetails principal,
			@Valid @RequestBody CreatePostRequest request) {
		List<Long> imageIds = parseLongIds(request.imageFileIds());
		Post post = postCommandService.create(principal.getUserId(), request.title(), request.content(), imageIds);
		return PostResponse.from(post, postQueryService.imageResponsesForPost(post.getId()));
	}

	@GetMapping("/{postId}")
	public PostResponse get(@PathVariable long postId) {
		return postQueryService.getDetail(postId);
	}

	@GetMapping
	public PostPageResponse list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return postQueryService.listPosts(page, size);
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
		return PostResponse.from(post, postQueryService.imageResponsesForPost(post.getId()));
	}

	@DeleteMapping("/{postId}")
	@PreAuthorize("isAuthenticated()")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable long postId) {
		postCommandService.delete(postId);
	}

	private static List<Long> parseLongIds(List<String> ids) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}
		return ids.stream().map(String::trim).filter(s -> !s.isEmpty()).map(Long::parseLong).toList();
	}
}
