package com.board.api.features.post.api;

/** 게시글 API 베이스 경로 */
// URL 문자열을 한 곳에 모아 오타 방지 + 변경 시 한 번만 수정
public final class PostApiPaths {

	// PostController 클래스의 @RequestMapping에 붙는 공통 prefix
	public static final String BASE = "/api/v1/posts";

	// 유틸 클래스: 인스턴스화(new) 방지
	private PostApiPaths() {
	}
}
