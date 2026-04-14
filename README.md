# 대규모 기술 게시판 앱

**백엔드**는 Java · Spring Boot로, **프론트엔드**는 React(TypeScript · Vite)로 구현합니다. 인프라와 로컬 실행은 **Docker / Docker Compose**를 기준으로 합니다. 아래 목차는 구현 범위의 로드맵이며, [docs/IMPLEMENTATION.md](docs/IMPLEMENTATION.md)에서 **프로젝트 세팅·단계별 구현 진행 상황**을 기록합니다.

| 항목 | 내용 |
|------|------|
| 백엔드 | Spring Boot 3.5 · Java 21 · Gradle (`backend/`) |
| 프론트 | React 19 · Vite 8 · TypeScript · Tailwind · shadcn/ui · Zustand · TanStack Query (`frontend/`) |
| 실행(프로덕션형) | `docker compose up --build` (MySQL + API + Nginx 정적 빌드) |
| 실행(개발·핫리로드) | `docker compose -f docker-compose.dev.yml up --build` |

---

## 저장소 구조

```
backend/
  src/main/java/com/board/api/
    BoardApiApplication.java
    common/                 # 횡단 관심사
      config/               # WebMvc 등
      exception/            # 전역 예외·에러 응답
    features/
      health/               # 헬스 도메인 (api / application / domain)
      post/                 # 게시글 도메인 (확장용 패키지)
frontend/
  src/
    app/                    # 앱 셸: App, Providers, QueryClient
    features/               # 도메인 기능 슬라이스 (예: health)
    widgets/                # 복합 UI (헤더 등)
    shared/                 # UI 키트, API 클라이언트, store, lib
docker-compose.yml
docker-compose.dev.yml
docs/IMPLEMENTATION.md
```

---

## 빠른 시작 (Docker)

```bash
docker compose up --build
```

- API: `http://localhost:8080/api/v1/health`
- UI: `http://localhost:5173` (Nginx가 `/api`를 백엔드로 프록시)
- MySQL(호스트에서 접속할 때): `localhost:3307` → 컨테이너 내부 3306 (호스트 3306 충돌 방지)

로컬에서만 개발할 때는 MySQL을 Compose로 띄운 뒤, 백엔드·프론트를 각각 실행할 수 있습니다. 자세한 절차는 [docs/IMPLEMENTATION.md](docs/IMPLEMENTATION.md)를 따릅니다.

### Docker 개발 모드 (핫리로드)

```bash
docker compose -f docker-compose.dev.yml up --build
```

- **프론트**: `http://localhost:5173` — Vite dev 서버, `frontend/` 저장 시 HMR
- **백엔드**: `http://localhost:8080` — `./backend`를 볼륨 마운트, Gradle `--continuous bootRun`으로 소스 변경 시 빌드 후 앱 재기동 (Spring DevTools + `application-dev.yml` 폴링)
- **API 프록시**: 브라우저는 여전히 `/api/...`만 호출하면 되고, Vite가 컨테이너 네트워크에서 `backend:8080`으로 넘깁니다.
- Docker Desktop(Mac 등)에서 감지가 느리면 프론트에 이미 `VITE_USE_POLLING=true`를 켜 두었습니다.
- 프론트 의존성은 **`frontend/node_modules`**(호스트와 공유)에 설치됩니다. 예전에 Compose가 쓰던 `frontend_dev_node_modules` 볼륨 때문에 패키지가 빠진 것처럼 보이면, 스택을 내린 뒤 `docker volume rm board-dev_frontend_dev_node_modules` 로 정리한 다음 다시 `up` 하세요.

---

## 앱에서 다루는 기능 개요

- **게시판 코어**: 게시글 CRUD, 페이지네이션·무한 스크롤, Snowflake·PK 전략
- **댓글**: 최대 2 depth / 무한 depth 설계 및 API
- **반응**: 좋아요·카운트, 게시글·댓글 수
- **조회수**: Redis, 어뷰징 방지
- **인기글·비동기**: Kafka, Transactional Outbox, Producer/Consumer
- **읽기 최적화**: CQRS, 캐시, 목록 최적화, Request Collapsing
- **파일**: 게시글 첨부 업로드·메타데이터·다운로드·삭제

---

## 구현 로드맵 (목차)

### 섹션 1. 기반 & 아키텍처

1. 대규모 시스템 서버 인프라 기초
2. 시스템 아키텍처 - Monolithic Architecture
3. 시스템 아키텍처 - Microservice Architecture
4. Docker
5. Spring Boot 프로젝트 세팅 1
6. Spring Boot 프로젝트 세팅 2

### 섹션 1-보충. 회원가입 · 인증 · 인가 (JWT)

1. 회원가입·로그인·로그아웃 API 및 검증(이메일·비밀번호 정책)
2. JWT 액세스 토큰(짧은 TTL)·리프레시 토(긴 TTL) 이중 구조
3. 리프레시 토큰 HttpOnly 쿠키 저장·서버측 해시(SHA-256) 보관·로그인 시 발급·갱신 시 회전(rotation)
4. Spring Security 필터 체인·CORS(credentials)·JSON 401/403
5. 역할 모델: 일반 사용자(USER)·관리자(ADMIN)·`@PreAuthorize` / `hasRole`
6. 관리자 전용 API(예: 사용자 목록) 및 부트스트랩 시드(개발용 초기 관리자)
7. 프론트: 액세스 토큰 메모리 보관·401 시 리프레시 1회 재시도·보호 라우트

### 섹션 2. 게시글

7. Distributed Relational Database
8. MySQL 개발 환경 세팅
9. 게시글 CRUD API 설계
10. Snowflake
11. 게시글 CRUD API 구현
12. 게시글 테스트 데이터 삽입
13. 게시글 목록 API - 페이지 번호 기반 설계 (N페이지 M개)
14. 게시글 목록 API - 전체 개수 기반 설계
15. 게시글 목록 API - 페이지 번호 구현
16. 게시글 목록 API - 무한 스크롤 설계
17. 게시글 목록 API - 무한 스크롤 구현
18. Primary Key 생성 전략

### 섹션 2-보충. 파일 업로드 (게시글 첨부)

19. 파일 업로드 요구사항 정리 - 용량 한도, 허용 MIME/확장자, 동시 첨부 개수
20. 첨부 저장소 선택 - 로컬 디스크 vs 객체 스토리지(S3 호환) 및 경로/키 네이밍 규칙
21. 첨부 메타데이터 테이블 설계 - `post_id`, 원본 파일명, 저장 키, 크기, MIME, 업로드 시각
22. 업로드 API 설계 - `multipart/form-data`, 게시글 작성/수정 시점과 트랜잭션 경계
23. Spring Boot 업로드 구현 - `MultipartFile`, 설정(`spring.servlet.multipart.*`), 예외 처리
24. 파일 저장 및 DB 반영 - 트랜잭션: 메타데이터 커밋 실패 시 오브젝트 롤백(삭제) 전략
25. 다운로드 API - 첨부 ID 기준 조회, `Content-Disposition`, 스트리밍/레인지(선택)
26. 삭제 및 정리 - 게시글 삭제 시 첨부 메타데이터·실파일 정합성, 고아 파일 정리(배치·선택)
27. 보안·운영 - 직링크 노출 최소화, 인증된 사용자만 업로드/다운로드, 바이러스 스캔 연동(선택)

### 섹션 3. 댓글

28. 댓글 최대 2 depth - 테이블 설계
29. 댓글 최대 2 depth - CUD API 구현
30. 댓글 최대 2 depth - 테스트 및 데이터 삽입
31. 댓글 최대 2 depth - 목록 API 설계
32. 댓글 최대 2 depth - 목록 API 구현
33. 댓글 무한 depth - 테이블 및 설계
34. 댓글 무한 depth - CUD API 구현 및 테스트 데이터 삽입
35. 댓글 무한 depth - 목록 API 구현

### 섹션 4. 좋아요

36. 좋아요 설계
37. 좋아요 구현
38. 좋아요 수 설계 - 테이블 정의
39. 좋아요 수 설계 - 동시성 문제
40. 좋아요 수 구현
41. 게시글 수 & 댓글 수 설계
42. 게시글 수 구현
43. 댓글 수 구현

### 섹션 5. 조회수

44. 조회수 설계 & Redis 개발 환경 세팅
45. 조회수 구현
46. 조회수 어뷰징 방지 정책 설계
47. 조회수 어뷰징 방지 정책 구현

### 섹션 6. 인기글

48. Kafka Cluster
49. Kafka 개발 환경 세팅
50. 인기글 Consumer 설계
51. Event & 직렬화 모듈 구현
52. 인기글 Consumer 구현 - 설정 및 리포지토리
53. 인기글 Consumer 구현 - 이벤트 데이터 리포지토리
54. 인기글 Consumer 구현 - 이벤트 핸들러 및 서비스 레이어
55. 인기글 Consumer 구현 - 컨트롤러 & 컨슈머 작성
56. 인기글 Producer 설계 - Transactional Messaging
57. Transactional Outbox 모듈 구현
58. Transactional Outbox 모듈 적용
59. Transactional Outbox 테스트
60. Producer & Consumer 테스트

### 섹션 7. 게시글 조회 최적화

61. CQRS의 필요성
62. CQRS 개념 및 조회 최적화 전략 설계
63. 조회 서비스 설정 & Client & QueryModel
64. 조회 최적화 구현 - 이벤트 핸들러 및 서비스 레이어
65. 조회 최적화 구현 - 조회수 캐시
66. 조회 최적화 구현 - Consumer & Controller
67. 조회 최적화 전략 테스트
68. 게시글 목록 최적화 전략 설계
69. 게시글 목록 최적화 구현 - Long & Double 테스트
70. 게시글 목록 최적화 구현 - 리포지토리
71. 게시글 목록 최적화 구현 - 서비스 & 컨트롤러
72. 게시글 목록 최적화 구현 - 테스트
73. 캐시 최적화 전략 설계 - 기존 전략의 한계
74. 캐시 최적화 전략 설계 - Request Collapsing
75. 캐시 최적화 전략 구현 - Request Collapsing
76. 캐시 최적화 전략 테스트
