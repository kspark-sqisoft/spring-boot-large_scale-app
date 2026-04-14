package com.board.api.features.auth.api.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.board.api.features.auth.domain.User;

public record UserPageResponse(
		List<UserSummaryResponse> content,
		int page,
		int size,
		long totalElements,
		int totalPages
) {

	public static UserPageResponse from(Page<User> page) {
		return new UserPageResponse(
				page.getContent().stream().map(UserSummaryResponse::from).toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages());
	}
}
