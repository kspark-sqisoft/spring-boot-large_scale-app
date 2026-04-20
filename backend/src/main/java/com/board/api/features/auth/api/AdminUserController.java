package com.board.api.features.auth.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.board.api.features.auth.api.dto.UserPageResponse;
import com.board.api.features.auth.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;

/** 관리자 전용 사용자 목록 페이징 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

	private static final int MAX_PAGE_SIZE = 100;

	private final UserRepository userRepository;

	@GetMapping("/users")
	public UserPageResponse listUsers(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		int safePage = Math.max(page, 0);
		Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
		return UserPageResponse.from(userRepository.findAll(pageable));
	}
}
