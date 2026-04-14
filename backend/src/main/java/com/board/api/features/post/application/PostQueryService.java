package com.board.api.features.post.application;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.board.api.common.exception.ApiException;
import com.board.api.features.comment.infrastructure.persistence.CommentRepository;
import com.board.api.features.file.api.FileApiPaths;
import com.board.api.features.post.api.dto.PostImageResponse;
import com.board.api.features.post.api.dto.PostPageResponse;
import com.board.api.features.post.api.dto.PostResponse;
import com.board.api.features.post.domain.Post;
import com.board.api.features.post.infrastructure.persistence.PostImageRepository;
import com.board.api.features.post.infrastructure.persistence.PostLikeRepository;
import com.board.api.features.post.infrastructure.persistence.PostRepository;

@Service
public class PostQueryService {

	private static final int MAX_PAGE_SIZE = 100;

	private final PostRepository postRepository;
	private final PostImageRepository postImageRepository;
	private final CommentRepository commentRepository;
	private final PostLikeRepository postLikeRepository;

	public PostQueryService(
			PostRepository postRepository,
			PostImageRepository postImageRepository,
			CommentRepository commentRepository,
			PostLikeRepository postLikeRepository) {
		this.postRepository = postRepository;
		this.postImageRepository = postImageRepository;
		this.commentRepository = commentRepository;
		this.postLikeRepository = postLikeRepository;
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
	public PostResponse buildResponse(Post post, Long viewerUserId) {
		long postId = post.getId();
		List<PostImageResponse> images = imageResponsesForPost(postId);
		long likes = postLikeRepository.countByPostId(postId);
		long comments = commentRepository.countByPostId(postId);
		boolean liked = viewerUserId != null
				&& postLikeRepository.existsByPostIdAndUserId(postId, viewerUserId);
		return PostResponse.from(post, images, likes, comments, liked);
	}

	@Transactional(readOnly = true)
	public PostResponse getDetail(long postId, Long viewerUserId) {
		Post post = getById(postId);
		return buildResponse(post, viewerUserId);
	}

	@Transactional(readOnly = true)
	public PostPageResponse listPosts(int page, int size, Long viewerUserId) {
		Page<Post> data = list(page, size);
		List<Post> content = data.getContent();
		if (content.isEmpty()) {
			return PostPageResponse.from(data, p -> buildResponse(p, viewerUserId));
		}
		List<Long> ids = content.stream().map(Post::getId).toList();
		Map<Long, Long> likeMap = toCountMap(postLikeRepository.countGroupedByPostId(ids));
		Map<Long, Long> commentMap = toCountMap(commentRepository.countGroupedByPostId(ids));
		Set<Long> likedSet = new HashSet<>();
		if (viewerUserId != null) {
			likedSet.addAll(postLikeRepository.findPostIdsLikedByUser(viewerUserId, ids));
		}
		return PostPageResponse.from(data, p -> {
			long pid = p.getId();
			long lc = likeMap.getOrDefault(pid, 0L);
			long cc = commentMap.getOrDefault(pid, 0L);
			boolean lk = likedSet.contains(pid);
			return PostResponse.from(p, imageResponsesForPost(pid), lc, cc, lk);
		});
	}

	private static Map<Long, Long> toCountMap(List<Object[]> rows) {
		Map<Long, Long> m = new HashMap<>();
		if (rows == null) {
			return m;
		}
		for (Object[] row : rows) {
			m.put((Long) row[0], (Long) row[1]);
		}
		return m;
	}
}
