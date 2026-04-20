package com.board.api.features.file.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.board.api.features.file.domain.StoredFile;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {

	// 본인 소유 파일만 조회 — 게시글 첨부·프로필 이미지 검증에 사용
	Optional<StoredFile> findByIdAndOwnerUserId(long id, long ownerUserId);
}
