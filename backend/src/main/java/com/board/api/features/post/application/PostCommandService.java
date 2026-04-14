package com.board.api.features.post.application;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.board.api.common.exception.ApiException;
import com.board.api.common.id.SnowflakeIdGenerator;
import com.board.api.features.post.domain.Post;
import com.board.api.features.post.domain.PostImage;
import com.board.api.features.post.infrastructure.persistence.PostImageRepository;
import com.board.api.features.post.infrastructure.persistence.PostRepository;
import com.board.api.features.file.infrastructure.persistence.StoredFileRepository;

@Service
public class PostCommandService {

	private final PostRepository postRepository;
	private final PostImageRepository postImageRepository;
	private final StoredFileRepository storedFileRepository;
	private final SnowflakeIdGenerator idGenerator;

	public PostCommandService(
			PostRepository postRepository,
			PostImageRepository postImageRepository,
			StoredFileRepository storedFileRepository,
			SnowflakeIdGenerator idGenerator) {
		this.postRepository = postRepository;
		this.postImageRepository = postImageRepository;
		this.storedFileRepository = storedFileRepository;
		this.idGenerator = idGenerator;
	}

	@Transactional
	public Post create(long ownerUserId, String title, String content, List<Long> imageFileIds) {
		List<Long> ids = imageFileIds == null ? List.of() : imageFileIds;
		validateFileOwnership(ownerUserId, ids);
		long id = idGenerator.nextId();
		Post post = Post.create(id, title, content);
		postRepository.save(post);
		attachImages(post.getId(), ids);
		return post;
	}

	@Transactional
	public Post update(long postId, long ownerUserId, String title, String content, List<Long> imageFileIdsOrNull) {
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다."));
		post.setTitle(title);
		post.setContent(content);
		post.touchUpdatedAt();
		postRepository.save(post);
		if (imageFileIdsOrNull != null) {
			validateFileOwnership(ownerUserId, imageFileIdsOrNull);
			postImageRepository.deleteByPostId(postId);
			attachImages(postId, imageFileIdsOrNull);
		}
		return post;
	}

	@Transactional
	public void delete(long postId) {
		if (!postRepository.existsById(postId)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다.");
		}
		postRepository.deleteById(postId);
	}

	private void validateFileOwnership(long ownerUserId, List<Long> fileIds) {
		for (Long fileId : fileIds) {
			storedFileRepository.findByIdAndOwnerUserId(fileId, ownerUserId)
					.orElseThrow(() -> new ApiException(
							HttpStatus.FORBIDDEN,
							"FILE_FORBIDDEN",
							"본인이 업로드한 이미지만 게시글에 첨부할 수 있습니다."));
		}
	}

	private void attachImages(long postId, List<Long> fileIds) {
		int order = 0;
		for (Long fileId : fileIds) {
			PostImage link = new PostImage(idGenerator.nextId(), postId, fileId, order++);
			postImageRepository.save(link);
		}
	}
}
