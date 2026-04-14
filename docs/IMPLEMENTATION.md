# 프로젝트 세팅 · 단계별 구현 기록

이 문서는 **Docker 기준**으로 백엔드(Spring Boot)·프론트(React)를 붙여 가며, README 로드맵 항목을 하나씩 완료할 때마다 체크·메모를 남기기 위한 장소입니다.

### 코드 구조 (요약)

| 영역 | 규칙 |
|------|------|
| 백엔드 | `common`(설정·예외) + `features.<도메인>`(api·application·domain·추후 infrastructure). 새 도메인은 `features` 아래 동일 패턴으로 추가. |
| 프론트 | FSD 지향: `app` → `widgets` → `features` → `shared`. 기능 코드는 `features/<도메인>/{api,model,ui}`, 재사용은 `shared`. |

---

## 현재 스택 (고정)

| 구분 | 선택 |
|------|------|
| API | Java 21, Spring Boot 3.5, Gradle, JPA, MySQL |
| UI | React 19, Vite, Tailwind, shadcn/ui(Radix), Zustand, TanStack Query, Sonner |
| 컨테이너 | Docker Compose: `mysql`, `backend`, `frontend` (`docker-compose.yml` / `docker-compose.dev.yml`) |

### 테스트 (요약)

| 구분 | 명령 | 비고 |
|------|------|------|
| 백엔드 단위·통합 | `cd backend && ./gradlew test` | H2, Mockito, MockMvc |
| 프론트 단위 | `cd frontend && npm run test` | Vitest, Testing Library |
| E2E | `cd e2e && npm test` | Playwright; 스택 기동 후 (`/api/v1/health` 200 아니면 스킵) |

에이전트·기여 시 테스트 정책은 `.cursor/rules/testing-requirements.mdc` 를 따릅니다.

---

## 단계 0 — 저장소 골격 (완료)

- [x] `backend/` Spring Boot 앱 (`com.board.api`), `/api/v1/health`
- [x] `frontend/` Vite React TS, 개발 시 `/api` → `localhost:8080` 프록시
- [x] `docker-compose.yml`: MySQL 8.4, API 빌드, Nginx로 빌드 산출물 + `/api` 리버스 프록시
- [x] 백엔드 테스트: H2 + `test` 프로파일로 컨텍스트 로드
- [x] CORS: `app.cors.allowed-origins`(쉼표 구분 문자열), Spring Security에서 `allowCredentials` 처리
- [x] `docker-compose.dev.yml`: 소스 볼륨 마운트, `backend/Dockerfile.dev` + `./gradlew --continuous bootRun`, 프론트는 Vite dev + `VITE_API_PROXY_TARGET` / 폴링 · `node_modules`는 `./frontend`에 두어 의존성 누락(Vite resolve 실패) 방지

**검증**

```bash
# 전체 스택
docker compose up --build

# UI에서 백엔드 health 확인: http://localhost:5173
curl -s http://localhost:8080/api/v1/health
```

```bash
# 개발(핫리로드)
docker compose -f docker-compose.dev.yml up --build
```

---

## 단계 1 — 인프라·아키텍처 정리 (선택·진행 예정)

로드맵 **섹션 1**에 맞춰 문서/다이어그램만 이 파일에 링크·요약으로 적어도 되고, 코드는 최소 변경으로 시작합니다.

- [ ] Monolith vs MSA 역할 분리 메모 (이 서비스는 현재 Monolith API + 별도 SPA)
- [ ] 이후 Kafka·Redis·CQRS 추가 시 Compose 서비스 항목 추가 계획

---

## 단계 2 — 게시글 도메인 (1차 완료)

- [x] Flyway `V1__create_posts.sql` + 운영 `ddl-auto: validate`, 테스트는 Flyway 끄고 H2 `create-drop`
- [x] Snowflake 스타일 ID: `common.id.SnowflakeIdGenerator` → API 응답 `id`는 JS 정밀도용 **문자열**
- [x] REST: `POST/GET/PUT/DELETE /api/v1/posts`, `GET /api/v1/posts?page&size` (최대 100건/페이지, 최신순)
- [x] 프론트: `/posts` 목록·페이지네이션, `/posts/new` 작성, `/posts/:id` 상세·삭제, `/posts/:id/edit` 수정

**API 요약**

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/v1/posts?page=0&size=20` | 페이지 목록 |
| POST | `/api/v1/posts` | JSON `{ "title", "content" }` |
| GET | `/api/v1/posts/{id}` | 상세 (`id`는 숫자 문자열) |
| PUT | `/api/v1/posts/{id}` | 수정 |
| DELETE | `/api/v1/posts/{id}` | 삭제 (204) |

**기존 MySQL 볼륨 주의**: 예전에 JPA `update`로만 쓰던 DB에 Flyway를 처음 붙일 때 스키마가 겹치면 볼륨 초기화 또는 수동 정리가 필요할 수 있음.

**다음(로드맵 후속)**: 커서 기반 무한 스크롤 API, 본문 길이·검색, Snowflake datacenter/worker 설정 외부화.

---

## 단계 2-보충 — 회원가입 · JWT 인증 · 역할(USER/ADMIN) (1차 완료)

- [x] Flyway `V2__users_and_refresh_tokens.sql` — `users`, `refresh_tokens`(해시·만료·revoked)
- [x] JWT(HMAC-SHA256): 액세스(기본 15분)·리프레시(기본 7일), jjwt 0.12
- [x] 리프레시: 랜덤 원문 → DB에는 SHA-256 hex, HttpOnly 쿠키 `board_rt`, `Path=/api/v1/auth`, `SameSite=Lax`, 갱신 시 회전
- [x] API: `POST /api/v1/auth/register|login|refresh|logout`, `GET /api/v1/users/me`, `GET /api/v1/admin/users`
- [x] 게시글 `POST/PUT/DELETE` 는 인증 필요, `GET` 목록·상세는 공개
- [x] `app.security.bootstrap-admin`: 개발 프로파일에서 초기 ADMIN 계정(이메일·비밀번호 설정 가능)
- [x] 프론트: Zustand 세션, `credentials: 'include'`, 401 시 `/auth/refresh` 1회 후 재시도, `/login`·`/register`, 작성/수정 라우트 보호, ADMIN만 `/admin/users`

**환경 변수 (백엔드)**

| 변수 | 설명 |
|------|------|
| `APP_JWT_SECRET` | JWT 서명용 시크릿(운영에서는 강한 랜덤 값) |
| `APP_CORS_ALLOWED_ORIGINS` | 쉼표 구분 origin (쿠키·credentials 대응) |
| `APP_SECURITY_BOOTSTRAP_ADMIN` | `true` 시 초기 관리자 1명 생성(이미 있으면 스킵) |
| `APP_SECURITY_INITIAL_ADMIN_EMAIL` / `APP_SECURITY_INITIAL_ADMIN_PASSWORD` | 부트스트랩 관리자 자격 |

**API 요약 (인증)**

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/v1/auth/register` | 가입 + 세션(액세스 JSON + 리프레시 Set-Cookie) |
| POST | `/api/v1/auth/login` | 로그인 + 동일 |
| POST | `/api/v1/auth/refresh` | 쿠키의 리프레시로 액세스 재발급·쿠키 회전 |
| POST | `/api/v1/auth/logout` | 리프레시 폐기·쿠키 삭제 |
| GET | `/api/v1/users/me` | 현재 사용자( Bearer ) |
| GET | `/api/v1/admin/users` | 사용자 페이지(ADMIN) |

---

## 단계 3 — 파일 업로드 · 프로필 이미지 · 게시글 이미지 (1차 완료)

- [x] Flyway `V4__files_and_profile_post_images.sql` — `stored_files`, `post_images`, 사용자 `display_name` / `avatar_file_id`
- [x] 로컬 디스크 저장(`APP_UPLOAD_DIR`), `POST /api/v1/uploads/image`, `GET /api/v1/files/{id}`(공개)
- [x] 게시글 작성·수정 시 `imageFileIds`(본인 업로드 파일만), 응답에 `images`
- [x] `GET/PATCH /api/v1/users/me` — 표시 이름·프로필 이미지
- [x] 프론트: 게시글 이미지 첨부·표시, 프로필 페이지, 헤더 링크
- [x] Compose: `APP_UPLOAD_DIR` — 개발은 호스트 `./data/uploads` 바인드 마운트(재시작·`down -v`로 이름 붙은 볼륨이 사라져도 업로드 유지), 프로덕션형은 `upload_data` 볼륨

**후속(로드맵 19~27 보충)**: 객체 스토리지(S3), 게시글 삭제 시 고아 파일 정리, 다운로드 `Content-Disposition`·권한 세분화 등.

---

## 단계 4 — 댓글 (최대 2 depth, 1차 완료)

로드맵 **섹션 3** 항목 28~32에 해당하는 1차 범위입니다.

- [x] Flyway `V5__post_comments.sql` — `post_comments`(게시글·부모 댓글·작성자·본문·시각)
- [x] 규칙: `parent_id` 가 없으면 루트, 루트에만 답글(2단계). 답글에 답글 시 `400 COMMENT_DEPTH_EXCEEDED`
- [x] API (게시글 하위 리소스):
  - `GET /api/v1/posts/{postId}/comments` — 목록(시간순), 공개
  - `POST` — 인증, 본문 + 선택 `parentCommentId`
  - `PUT /api/v1/posts/{postId}/comments/{commentId}` — 본인만
  - `DELETE` — 본인만 (루트 삭제 시 답글은 FK CASCADE로 함께 삭제)
- [x] 프론트: 게시글 상세 하단 댓글 카드 — 목록·루트 작성·답글·수정·삭제
- [x] 테스트: `CommentEndpointTests` (MockMvc + 회원가입 JWT)

**다음(로드맵 33~35)**: 무한 depth 댓글 모델·API(별도 설계).

---

## 단계 5 — 좋아요 · 게시글 응답에 댓글 수 (1차 완료)

로드맵 **섹션 4** 항목 36~43 중, 게시글 좋아요·집계·표시까지 1차로 반영했습니다.

- [x] Flyway `V6__post_likes.sql` — `post_likes` (`post_id`, `user_id` UNIQUE, 게시글 삭제 시 CASCADE)
- [x] `POST /api/v1/posts/{postId}/likes` — 좋아요 (멱등: 이미 있으면 동일 상태 반환). 동시 요청으로 UNIQUE 충돌 시 예외 삼키고 재조회
- [x] `DELETE /api/v1/posts/{postId}/likes` — 좋아요 취소 (없어도 200 + 현재 카운트)
- [x] `PostResponse`에 `likeCount`, `commentCount`, `likedByMe`(로그인 시만 true 가능) — 목록·상세에서 배치 집계(페이지 내 `postId IN (...)`)
- [x] 프론트: 목록/상세에 좋아요·댓글 수, 로그인 시 좋아요 토글
- [x] 댓글 변경 시 게시글 쿼리 무효화로 `commentCount` 갱신
- [x] 테스트: `PostLikeEndpointTests`

**후속(로드맵 38~40 보충)**: 역할 기반 캐시·Redis 카운터·비관적 락 등 고동시 시나리오 튜닝.

**로드맵 41~43(게시글 수 & 댓글 수)**: 사용자별 작성 글/댓글 수 집계 API는 미구현(필요 시 별도 엔드포인트).

---

## 단계 6 — 조회수 · Redis (1차 완료)

로드맵 **섹션 5** 항목 44~45에 해당하는 1차 범위(카운터 저장·표시)입니다. 어뷰징 방지(46~47)는 후속입니다.

- [x] `spring-boot-starter-data-redis` + 기본 `RedisAutoConfiguration` 제외 후, `app.redis.enabled=true` 일 때만 Lettuce `StringRedisTemplate` 구성 (`RedisPostViewConfiguration`)
- [x] `PostViewService`: Redis 시 `INCR board:views:post:{id}` / `MGET` 배치, 비활성 시 `NoopPostViewService`(항상 0)
- [x] `GET /api/v1/posts/{id}` 한 번 호출마다 조회수 증가 후 응답의 `viewCount`에 반영 · 목록은 증가 없이 Redis 값만 조회
- [x] `application.yml`: `APP_REDIS_ENABLED`, `APP_REDIS_VIEW_HOST` / `PORT` — 테스트는 `application-test.yml`에서 `app.redis.enabled: false`
- [x] Compose: `redis:7.4-alpine`, 백엔드 `depends_on` + `APP_REDIS_ENABLED=true` (`docker-compose.dev.yml`, `docker-compose.yml`)
- [x] 프론트: `PostDto.viewCount`, 목록·상세 메타에 조회 수 표시

**후속**: 동일 사용자/세션·IP 윈도우 내 중복 집계 방지, Redis 장애 시 폴백, MySQL 비동기 동기화 등.

---

## 단계 7 — 인기글 · Kafka (1차 완료)

로드맵 **섹션 6**에 가까운 1차 범위: 조회 이벤트를 Kafka로 발행하고, Consumer가 Redis ZSET으로 점수를 올린 뒤 `GET /posts/popular`로 조회합니다.

- [x] `spring-kafka`, `KafkaAutoConfiguration` 제외 + `app.kafka.enabled=true` 일 때만 `KafkaBoardConfiguration` (`KafkaTemplate`, consumer factory, `NewTopic` `board.post.viewed`)
- [x] `PostViewEventPublisher` — Kafka off 시 `NoopPostViewEventPublisher`, on 시 `KafkaPostViewEventPublisher` (`PostViewedEvent` JSON)
- [x] `GET /api/v1/posts/{id}` 응답 직전 `publishPostViewed` (조회수 증가와 별도 파이프라인)
- [x] `PostViewKafkaConsumer` → `PopularPostsScoreWriter` — Redis on 시 `ZINCRBY board:popular:posts`, off 시 No-op
- [x] `GET /api/v1/posts/popular?limit=` — Redis on일 때 ZSET 역순으로 `Post` 로드(없으면 `[]`). 라우팅 순서: `/popular`를 `/{postId}` 위에 둠
- [x] `application.yml` / `application-test.yml`: `APP_KAFKA_*`, 테스트는 Kafka off
- [x] `docker-compose.dev.yml`: Redpanda + `APP_KAFKA_ENABLED=true`, `APP_KAFKA_BOOTSTRAP_SERVERS=redpanda:9092`. 기본 `docker-compose.yml`은 Kafka 기본 off, 브로커는 필요 시 별도 기동
- [x] 프론트: `/posts/popular`, `fetchPopularPosts`, 헤더「인기」
- [x] 테스트: `PostPopularEndpointTests` (Redis off 시 빈 배열)

**후속**: 토픽 파티션·재처리·DLQ, 인기 점수 가중치(좋아요 등), Kafka 미가용 시 백프레셔/폴백.

---

## 단계 8 이후

CQRS·캐시 등 로드맵 후속 단계는 매 단계마다 **Compose**와 **이 문서**를 함께 갱신합니다.

---

## 로컬 개발 (Compose 없이)

1. MySQL만 Compose로 실행: `docker compose up mysql -d` (호스트에서 접속 시 **3307** → 컨테이너 3306)
2. 백엔드: `cd backend && ./gradlew bootRun` — 로컬 DB가 3306이면 그대로 두고, Compose MySQL만 쓸 경우 `application.yml`의 URL을 `localhost:3307`로 맞출 것. 조회수 Redis를 쓰려면 로컬 Redis + `APP_REDIS_ENABLED=true`(또는 Compose 풀스택).
3. 프론트: `cd frontend && npm run dev` → `http://localhost:5173` (Vite가 `/api` 프록시)

---

## 메모

| 날짜 | 내용 |
|------|------|
| 2026-04-14 | 초기 스택 구동 확인: `docker compose up --build`, MySQL 호스트 포트 **3307** (로컬 3306 충돌 회피), `/api/v1/health` 직접·Nginx 프록시 모두 OK |
| 2026-04-14 | 로드맵 **섹션 3(댓글)** 1차: Flyway `V5`, `post_comments`, REST CRUD·2-depth 제한, 상세 페이지 댓글 UI, `CommentEndpointTests` |
| 2026-04-14 | 로드맵 **섹션 4(좋아요)** 1차: Flyway `V6`, `post_likes`, POST/DELETE likes, `PostResponse` 집계 필드, `PostLikeEndpointTests` |
| 2026-04-14 | 로드맵 **섹션 5(조회수)** 1차: Redis 조회 카운터, `viewCount`, Compose `redis` 서비스, 비활성 시 No-op |
| 2026-04-14 | 로드맵 **섹션 6(인기·Kafka)** 1차: `PostViewed` 토픽, Redis ZSET 인기, `GET /posts/popular`, dev Compose Redpanda |
