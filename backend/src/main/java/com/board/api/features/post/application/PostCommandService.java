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
// @Service: 스프링이 컴포넌트 스캔으로 Bean 등록 + 일반적으로 @Transactional과 함께 쓰는 서비스 레이어
@Service
@RequiredArgsConstructor
public class PostCommandService {

	// JPA Repository: Post 엔티티 CRUD
	private final PostRepository postRepository;
	// 게시글 ↔ 업로드 파일 연결 테이블(post_images) 담당
	private final PostImageRepository postImageRepository;
	// 업로드 파일 메타데이터 (소유자 userId 확인용)
	private final StoredFileRepository storedFileRepository;
	// DB auto increment 대신 애플리케이션에서 PK 생성
	private final SnowflakeIdGenerator idGenerator;

	// @Transactional: 메서드 시작~끝을 하나의 DB 트랜잭션으로 묶음 (실패 시 롤백)
	@Transactional
	public Post create(long ownerUserId, String title, String content, List<Long> imageFileIds) {
		List<Long> ids = imageFileIds == null ? List.of() : imageFileIds;
		// 다른 사용자가 올린 파일 ID를 몰래 끼워 넣는 것 방지
		validateFileOwnership(ownerUserId, ids);
		long id = idGenerator.nextId();
		Post post = Post.create(id, ownerUserId, title, content);
		postRepository.save(post);
		// 첨부 순서(sort_order)를 유지하며 post_images 행 삽입
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
		// null이면 이미지 구성은 변경하지 않음(요청에서 생략한 의미)
		if (imageFileIdsOrNull != null) {
			validateFileOwnership(actorUserId, imageFileIdsOrNull);
			// 전체 교체: 기존 매핑 삭제 후 재삽입
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
		// FK CASCADE 설정에 따라 댓글·첨부 등은 DB 스키마가 정리
		postRepository.deleteById(postId);
	}

	// 작성자 본인 또는 ADMIN만 수정/삭제 허용
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

	// StoredFile.ownerUserId 가 현재 사용자와 일치하는지 각 파일마다 확인
	private void validateFileOwnership(long ownerUserId, List<Long> fileIds) {
		for (Long fileId : fileIds) {
			storedFileRepository.findByIdAndOwnerUserId(fileId, ownerUserId)
					.orElseThrow(() -> new ApiException(
							HttpStatus.FORBIDDEN,
							"FILE_FORBIDDEN",
							"본인이 업로드한 이미지만 게시글에 첨부할 수 있습니다."));
		}
	}

	// PostImage 엔티티: (postId, fileId, sortOrder) 조합으로 저장
	private void attachImages(long postId, List<Long> fileIds) {
		int order = 0;
		for (Long fileId : fileIds) {
			PostImage link = new PostImage(idGenerator.nextId(), postId, fileId, order++);
			postImageRepository.save(link);
		}
	}
}
