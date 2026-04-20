package com.board.api.features.post.application;

import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.board.api.common.exception.ApiException;
import com.board.api.common.id.SnowflakeIdGenerator;
import com.board.api.features.auth.domain.UserRole;
import com.board.api.features.post.domain.Post;
import com.board.api.features.post.domain.PostImage;
import com.board.api.features.post.infrastructure.persistence.PostImageRepository;
import com.board.api.features.post.infrastructure.persistence.PostRepository;
import com.board.api.features.file.infrastructure.persistence.StoredFileRepository;
import lombok.RequiredArgsConstructor;

/** 게시글·첨부 이미지 연결의 트랜잭션 단위 변경(생성·수정·삭제) */
@Service
@RequiredArgsConstructor
public class PostCommandService {

	private final PostRepository postRepository;
	private final PostImageRepository postImageRepository;
	private final StoredFileRepository storedFileRepository;
	private final SnowflakeIdGenerator idGenerator;

	@Transactional
	public Post create(long ownerUserId, String title, String content, List<Long> imageFileIds) {
		List<Long> ids = imageFileIds == null ? List.of() : imageFileIds;
		validateFileOwnership(ownerUserId, ids);
		long id = idGenerator.nextId();
		Post post = Post.create(id, ownerUserId, title, content);
		postRepository.save(post);
		attachImages(post.getId(), ids);
		return post;
	}

	@Transactional
	public Post update(
			long postId,
			long actorUserId,
			UserRole actorRole,
			String title,
			String content,
			List<Long> imageFileIdsOrNull) {
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다."));
		assertCanModifyPost(post, actorUserId, actorRole);
		post.setTitle(title);
		post.setContent(content);
		post.touchUpdatedAt();
		postRepository.save(post);
		if (imageFileIdsOrNull != null) {
			validateFileOwnership(actorUserId, imageFileIdsOrNull);
			postImageRepository.deleteByPostId(postId);
			attachImages(postId, imageFileIdsOrNull);
		}
		return post;
	}

	@Transactional
	public void delete(long postId, long actorUserId, UserRole actorRole) {
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다."));
		assertCanModifyPost(post, actorUserId, actorRole);
		postRepository.deleteById(postId);
	}

	private static void assertCanModifyPost(Post post, long actorUserId, UserRole actorRole) {
		if (actorRole == UserRole.ADMIN) {
			return;
		}
		Long author = post.getAuthorUserId();
		if (author == null) {
			return;
		}
		if (!Objects.equals(author, actorUserId)) {
			throw new ApiException(
					HttpStatus.FORBIDDEN,
					"POST_FORBIDDEN",
					"이 게시글을 수정하거나 삭제할 권한이 없습니다.");
		}
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
