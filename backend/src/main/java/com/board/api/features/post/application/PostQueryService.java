package com.board.api.features.post.application;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.board.api.common.exception.ApiException;
import com.board.api.features.comment.infrastructure.persistence.CommentRepository;
import com.board.api.features.file.api.FileApiPaths;
import com.board.api.features.post.api.dto.PostCursorPageResponse;
import com.board.api.features.post.api.dto.PostImageResponse;
import com.board.api.features.post.api.dto.PostResponse;
import com.board.api.features.post.domain.Post;
import com.board.api.features.post.infrastructure.persistence.PostImageRepository;
import com.board.api.features.post.infrastructure.persistence.PostLikeRepository;
import com.board.api.features.post.infrastructure.persistence.PostRepository;

/** 게시글 조회·목록(커서)·응답 DTO 조립(작성자·이미지·좋아요·댓글 수 등) */
@Service
public class PostQueryService {

	private static final int MAX_PAGE_SIZE = 100;

	private final PostRepository postRepository;
	private final PostImageRepository postImageRepository;
	private final CommentRepository commentRepository;
	private final PostLikeRepository postLikeRepository;
	private final PostViewService postViewService;

	public PostQueryService(
			PostRepository postRepository,
			PostImageRepository postImageRepository,
			CommentRepository commentRepository,
			PostLikeRepository postLikeRepository,
			PostViewService postViewService) {
		this.postRepository = postRepository;
		this.postImageRepository = postImageRepository;
		this.commentRepository = commentRepository;
		this.postLikeRepository = postLikeRepository;
		this.postViewService = postViewService;
	}

	@Transactional(readOnly = true)
	public Post getById(long postId) {
		return postRepository.findById(postId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다."));
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
		long views = postViewService.getCount(post.getId());
		return buildResponse(post, viewerUserId, views);
	}

	@Transactional(readOnly = true)
	public PostResponse buildResponse(Post post, Long viewerUserId, long viewCount) {
		long postId = post.getId();
		List<PostImageResponse> images = imageResponsesForPost(postId);
		long likes = postLikeRepository.countByPostId(postId);
		long comments = commentRepository.countByPostId(postId);
		boolean liked = viewerUserId != null
				&& postLikeRepository.existsByPostIdAndUserId(postId, viewerUserId);
		return PostResponse.from(post, images, likes, comments, liked, viewCount);
	}

	@Transactional(readOnly = true)
	public PostResponse getDetailWithViewCount(long postId, Long viewerUserId, long viewCount) {
		Post post = getById(postId);
		return buildResponse(post, viewerUserId, viewCount);
	}

	/**
	 * 키셋 페이지네이션: 최신순(작성 시각 내림차순, 동률 시 id 내림차순).
	 *
	 * @param cursorEncoded 직전 페이지 마지막 글의 커서(없으면 첫 페이지)
	 */
	@Transactional(readOnly = true)
	public PostCursorPageResponse listPostsByCursor(String cursorEncoded, int size, Long viewerUserId) {
		int limit = clampPageSize(size);
		int fetch = limit + 1;
		Pageable pageable = PageRequest.of(0, fetch);

		List<Post> chunk;
		if (cursorEncoded == null || cursorEncoded.isBlank()) {
			chunk = postRepository.findAllByOrderByCreatedAtDescIdDesc(pageable).getContent();
		}
		else {
			PostCursorCodec.Cursor c = PostCursorCodec.decode(cursorEncoded);
			chunk = postRepository.findOlderThan(c.createdAt(), c.postId(), pageable);
		}

		boolean hasMore = chunk.size() > limit;
		List<Post> page = hasMore ? chunk.subList(0, limit) : chunk;
		String nextCursor = null;
		if (hasMore && !page.isEmpty()) {
			Post last = page.get(page.size() - 1);
			nextCursor = PostCursorCodec.encode(last.getCreatedAt(), last.getId());
		}

		List<PostResponse> mapped = mapPostsWithBatchAggregates(page, viewerUserId);
		return new PostCursorPageResponse(mapped, nextCursor, mapped.size(), hasMore);
	}

	private List<PostResponse> mapPostsWithBatchAggregates(List<Post> posts, Long viewerUserId) {
		if (posts.isEmpty()) {
			return List.of();
		}
		List<Long> ids = posts.stream().map(Post::getId).toList();
		Map<Long, Long> likeMap = toCountMap(postLikeRepository.countGroupedByPostId(ids));
		Map<Long, Long> commentMap = toCountMap(commentRepository.countGroupedByPostId(ids));
		Map<Long, Long> viewMap = postViewService.getCounts(ids);
		Set<Long> likedSet = new HashSet<>();
		if (viewerUserId != null) {
			likedSet.addAll(postLikeRepository.findPostIdsLikedByUser(viewerUserId, ids));
		}
		return posts.stream().map(p -> {
			long pid = p.getId();
			long lc = likeMap.getOrDefault(pid, 0L);
			long cc = commentMap.getOrDefault(pid, 0L);
			long vc = viewMap.getOrDefault(pid, 0L);
			boolean lk = likedSet.contains(pid);
			return PostResponse.from(p, imageResponsesForPost(pid), lc, cc, lk, vc);
		}).toList();
	}

	private static int clampPageSize(int size) {
		return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
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
