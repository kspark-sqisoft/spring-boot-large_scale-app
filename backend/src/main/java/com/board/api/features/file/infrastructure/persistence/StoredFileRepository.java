package com.board.api.features.file.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.board.api.features.file.domain.StoredFile;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {

	Optional<StoredFile> findByIdAndOwnerUserId(long id, long ownerUserId);
}
