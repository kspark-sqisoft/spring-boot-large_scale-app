package com.board.api.features.post.api;

import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.board.api.common.security.AppUserDetails;
import com.board.api.features.post.api.dto.CreatePostRequest;
import com.board.api.features.post.api.dto.PostLikeStatusResponse;
import com.board.api.features.post.api.dto.PostCursorPageResponse;
import com.board.api.features.post.api.dto.PostResponse;
import com.board.api.features.post.api.dto.UpdatePostRequest;
import com.board.api.features.post.application.PopularPostsQueryService;
import com.board.api.features.post.application.PostCommandService;
import com.board.api.features.post.application.PostLikeCommandService;
import com.board.api.features.post.application.PostQueryService;
import com.board.api.features.post.application.PostViewEventPublisher;
import com.board.api.features.post.application.PostViewService;
import com.board.api.features.post.domain.Post;
import lombok.RequiredArgsConstructor;

/** 게시글 CRUD·목록(커서)·인기·조회수·좋아요. 조회는 대부분 공개, 쓰기는 인증 필요. */
// @RestController: 이 클래스의 메서드 반환값이 HTTP 응답 본문(JSON)으로 직렬화됨 (@Controller + @ResponseBody)
// @RequestMapping: 이 컨트롤러 전체에 공통으로 붙는 URL 접두사 (PostApiPaths.BASE = /api/v1/posts)
// @RequiredArgsConstructor: final 필드만 모아 생성자를 만들어줌 → Spring이 생성자 주입으로 서비스 빈들을 넣어줌
@RestController
@RequestMapping(PostApiPaths.BASE)
@RequiredArgsConstructor
public class PostController {

	// 게시글 생성·수정·삭제 같은 "명령" 유스케이스
	private final PostCommandService postCommandService;
	// 조회·목록·DTO 조립 같은 "질의" 유스케이스
	private final PostQueryService postQueryService;
	// 좋아요 토글(추가/취소) 전용
	private final PostLikeCommandService postLikeCommandService;
	// 조회수 카운터(Redis 또는 No-op 구현체가 주입됨)
	private final PostViewService postViewService;
	// 조회 이벤트를 Kafka 등으로 보낼 때 사용(구현체가 없으면 No-op)
	private final PostViewEventPublisher postViewEventPublisher;
	// 인기글 목록(정렬된 점수 테이블 등에서 읽음)
	private final PopularPostsQueryService popularPostsQueryService;

	// POST /api/v1/posts — 새 글 작성 (JSON 바디)
	@PostMapping
	// @PreAuthorize: 메서드 진입 전에 Spring Security 표현식으로 권한 검사 (로그인 필수)
	@PreAuthorize("isAuthenticated()")
	// @ResponseStatus: 성공 시 기본 HTTP 상태를 201 Created로 지정 (기본 200이 아님)
	@ResponseStatus(HttpStatus.CREATED)
	public PostResponse create(
			// JWT 필터가 넣어 둔 로그인 사용자 정보 (SecurityContext → AppUserDetails)
			@AuthenticationPrincipal AppUserDetails principal,
			// @Valid: CreatePostRequest 안의 @NotBlank 등 검증 어노테이션 실행, 실패 시 400
			// @RequestBody: HTTP 본문(JSON)을 CreatePostRequest record로 역직렬화
			@Valid @RequestBody CreatePostRequest request) {
		// 클라이언트가 문자열 ID 목록으로 보낸 업로드 파일 ID들을 Long 리스트로 변환
		List<Long> imageIds = parseLongIds(request.imageFileIds());
		// 트랜잭션 경계는 서비스(@Transactional) 안에서 처리됨
		Post post = postCommandService.create(principal.getUserId(), request.title(), request.content(), imageIds);
		// 저장된 엔티티를 API 응답 DTO로 변환 (이미지 URL·좋아요 수 등 포함)
		return postQueryService.buildResponse(post, principal.getUserId());
	}

	// GET /api/v1/posts/popular?limit=10 — 인기글 목록 (로그인 없이도 가능, viewerId는 있으면 "내가 좋아요 했는지" 등에 사용)
	@GetMapping("/popular")
	public List<PostResponse> popular(
			// @RequestParam: 쿼리스트링 ?limit=20 형태로 받음, defaultValue로 생략 시 10
			@RequestParam(defaultValue = "10") int limit,
			// 로그인 여부와 관계없이 Authentication 객체는 올 수 있음(익명 포함)
			Authentication authentication) {
		return popularPostsQueryService.listPopular(limit, viewerId(authentication));
	}

	// GET /api/v1/posts/{postId} — 단건 상세
	@GetMapping("/{postId}")
	public ResponseEntity<PostResponse> get(
			// @PathVariable: URL 경로의 {postId} 부분을 long으로 바인딩
			@PathVariable long postId,
			Authentication authentication) {
		// Redis(옵션) 조회수 증가 + Kafka(옵션) 조회 이벤트
		// incrementAndGet: 이 요청으로 조회수가 1 올라간 뒤의 값을 반환(구현에 따라 다를 수 있음)
		long viewCount = postViewService.incrementAndGet(postId);
		// 비동기 분석·배치용으로 "누가 언제 봤는지" 이벤트만 발행 (본 응답과는 느슨하게 결합)
		postViewEventPublisher.publishPostViewed(postId);
		// DB에서 글 + 집계 정보를 읽어 PostResponse로 조립 (방금 계산한 viewCount 반영)
		PostResponse body = postQueryService.getDetailWithViewCount(postId, viewerId(authentication), viewCount);
		// ResponseEntity: 상태코드·헤더·바디를 함께 제어할 때 사용
		return ResponseEntity.ok()
				// 민감·개인화된 내용이 있을 수 있어 브라우저/중간 캐시에 저장하지 않음
				.cacheControl(CacheControl.noStore())
				.body(body);
	}

	// GET /api/v1/posts?cursor=...&size=20 — 커서 기반 목록 (무한 스크롤에 적합)
	@GetMapping
	public ResponseEntity<PostCursorPageResponse> list(
			// required=false: 첫 페이지는 cursor 없이 호출 가능
			@RequestParam(required = false) String cursor,
			@RequestParam(defaultValue = "20") int size,
			Authentication authentication) {
		PostCursorPageResponse body = postQueryService.listPostsByCursor(cursor, size, viewerId(authentication));
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.body(body);
	}

	// PUT /api/v1/posts/{postId} — 수정 (작성자 또는 관리자)
	@PutMapping("/{postId}")
	@PreAuthorize("isAuthenticated()")
	public PostResponse update(
			@AuthenticationPrincipal AppUserDetails principal,
			@PathVariable long postId,
			@Valid @RequestBody UpdatePostRequest request) {
		// imageFileIds가 null이면 "이미지 목록은 그대로" 의미(서비스에서 분기)
		List<Long> imageIdsOrNull = request.imageFileIds() == null
				? null
				: parseLongIds(request.imageFileIds());
		Post post = postCommandService.update(
				postId,
				principal.getUserId(),
				principal.getRole(),
				request.title(),
				request.content(),
				imageIdsOrNull);
		return postQueryService.buildResponse(post, principal.getUserId());
	}

	// DELETE /api/v1/posts/{postId} — 삭제 (작성자 또는 관리자)
	@DeleteMapping("/{postId}")
	@PreAuthorize("isAuthenticated()")
	// 204 No Content: 성공했지만 응답 본문이 없음을 나타냄
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(
			@AuthenticationPrincipal AppUserDetails principal,
			@PathVariable long postId) {
		postCommandService.delete(postId, principal.getUserId(), principal.getRole());
	}

	// POST /api/v1/posts/{postId}/likes — 좋아요 추가
	@PostMapping("/{postId}/likes")
	@PreAuthorize("isAuthenticated()")
	public PostLikeStatusResponse addLike(
			@AuthenticationPrincipal AppUserDetails principal,
			@PathVariable long postId) {
		return postLikeCommandService.like(postId, principal.getUserId());
	}

	// DELETE /api/v1/posts/{postId}/likes — 좋아요 취소
	@DeleteMapping("/{postId}/likes")
	@PreAuthorize("isAuthenticated()")
	public PostLikeStatusResponse removeLike(
			@AuthenticationPrincipal AppUserDetails principal,
			@PathVariable long postId) {
		return postLikeCommandService.unlike(postId, principal.getUserId());
	}

	// Security의 Authentication에서 "로그인한 실제 사용자"의 userId만 꺼내는 헬퍼
	private static Long viewerId(Authentication authentication) {
		// 익명 사용자(AnonymousAuthenticationToken)는 로그인한 것으로 치지 않음
		if (authentication == null
				|| !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return null;
		}
		Object p = authentication.getPrincipal();
		// JWT 필터가 AppUserDetails를 principal로 넣었을 때만 ID 반환
		if (p instanceof AppUserDetails details) {
			return details.getUserId();
		}
		return null;
	}

	// 프론트가 보낸 문자열 ID 배열을 Long 리스트로 안전하게 변환 (빈 문자열 제거, trim)
	private static List<Long> parseLongIds(List<String> ids) {
		if (ids == null || ids.isEmpty()) {
			return List.of();
		}
		return ids.stream().map(String::trim).filter(s -> !s.isEmpty()).map(Long::parseLong).toList();
	}
}
