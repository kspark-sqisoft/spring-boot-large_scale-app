package com.board.api.features.post.application;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.board.api.common.exception.ApiException;
import com.board.api.features.file.api.FileApiPaths;
import com.board.api.features.post.api.dto.PostImageResponse;
import com.board.api.features.post.api.dto.PostPageResponse;
import com.board.api.features.post.api.dto.PostResponse;
import com.board.api.features.post.domain.Post;
import com.board.api.features.post.infrastructure.persistence.PostImageRepository;
import com.board.api.features.post.infrastructure.persistence.PostRepository;

@Service
public class PostQueryService {

	private static final int MAX_PAGE_SIZE = 100;

	private final PostRepository postRepository;
	private final PostImageRepository postImageRepository;

	public PostQueryService(PostRepository postRepository, PostImageRepository postImageRepository) {
		this.postRepository = postRepository;
		this.postImageRepository = postImageRepository;
	}

	@Transactional(readOnly = true)
	public Post getById(long postId) {
		return postRepository.findById(postId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다."));
	}

	@Transactional(readOnly = true)
	public Page<Post> list(int page, int size) {
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		int safePage = Math.max(page, 0);
		Pageable pageable = PageRequest.of(safePage, safeSize);
		return postRepository.findAllByOrderByCreatedAtDesc(pageable);
	}

	@Transactional(readOnly = true)
	public List<PostImageResponse> imageResponsesForPost(long postId) {
		return postImageRepository.findByPostIdOrderBySortOrderAsc(postId).stream()
				.map(pi -> new PostImageResponse(
						Long.toString(pi.getFileId()),
						FileApiPaths.FILES + "/" + pi.getFileId()))
				.toList();
	}

	@Transactional(readOnly = true)
	public PostResponse getDetail(long postId) {
		Post post = getById(postId);
		return PostResponse.from(post, imageResponsesForPost(postId));
	}

	@Transactional(readOnly = true)
	public PostPageResponse listPosts(int page, int size) {
		Page<Post> data = list(page, size);
		return PostPageResponse.from(data, p -> PostResponse.from(p, imageResponsesForPost(p.getId())));
	}
}
