package com.board.api.features.auth.domain;

/**
 * 사용자 역할(권한) enum.
 * DB에는 문자열 "USER" 또는 "ADMIN"으로 저장됩니다.
 * Spring Security에서는 "ROLE_USER", "ROLE_ADMIN" 형태로 사용됩니다.
 */
public enum UserRole {
	USER,  // 일반 회원: 게시글·댓글 CRUD, 파일 업로드 등
	ADMIN  // 관리자: 모든 게시글·사용자 관리 가능 (/api/v1/admin/**)
}
