# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

Spring Boot 3.5 (Java 21) + React 19 (TypeScript) 기반의 대규모 게시판 애플리케이션.
MySQL, Redis, Kafka(Redpanda)를 인프라로 사용하며 Docker Compose로 개발/운영 환경을 분리한다.

---

## 명령어

### 개발 환경 실행 (Docker)

```bash
# 전체 스택 실행 (라이브 리로드)
docker compose -f docker-compose.dev.yml up --build

# 백그라운드 실행
docker compose -f docker-compose.dev.yml up -d
```

### 백엔드 (backend/)

```bash
./gradlew bootRun          # 로컬 실행
./gradlew bootJar          # JAR 빌드
./gradlew test             # 전체 테스트 (H2 인메모리, Redis/Kafka 없음)
./gradlew test --tests "com.board.api.features.post.*"  # 특정 패키지 테스트
```

테스트 리포트: `backend/build/reports/tests/test/index.html`

### 프론트엔드 (frontend/)

```bash
npm install
npm run dev          # Vite 개발 서버 (HMR)
npm run build        # TypeScript 검사 + Vite 빌드
npm run test         # Vitest 1회 실행
npm run test:watch   # Vitest 감시 모드
npm run test:coverage
npm run lint
```

### E2E 테스트 (e2e/)

```bash
# 사전 조건: docker compose -f docker-compose.dev.yml up 실행 중
cd e2e && npm install && npx playwright install chromium
npm test
npm run report  # HTML 리포트 열기
```

---

## 아키텍처

### 백엔드 구조

```
backend/src/main/java/com/board/api/
├── common/
│   ├── config/      # Security, JWT, Redis, Kafka, CORS, 파일스토리지 설정
│   ├── exception/   # 글로벌 예외 핸들러, ApiException
│   ├── id/          # Snowflake ID 생성기
│   └── security/    # JwtTokenProvider, JwtAuthenticationFilter, UserDetailsService
└── features/
    ├── auth/        # 로그인, 회원가입, 토큰 갱신
    ├── post/        # 게시글 CRUD, 좋아요, 조회수
    ├── comment/     # 댓글 (2단계 깊이 + 무한 깊이 두 가지 모드)
    ├── file/        # 파일 업로드/다운로드
    └── health/      # DB/Redis/Kafka 연결 상태 확인
```

각 feature는 `controller → service → repository` 레이어로 구성된다.

### 프론트엔드 구조 (Feature-Sliced Design)

```
frontend/src/
├── app/       # 앱 진입점, Provider, Router, QueryClient
├── features/  # 도메인별 기능 (auth, post, comment, file, health)
├── widgets/   # 복합 UI 컴포넌트 (app-header, app-layout)
└── shared/
    ├── api/   # HTTP 클라이언트 (인증 재시도, 동시 401 병합)
    ├── store/ # Zustand 인증 상태
    └── ui/    # shadcn/ui 공통 컴포넌트
```

---

## 핵심 설계 결정

### 인증 (Dual-Token)
- **Access Token**: 메모리에 보관 (15분), 요청 헤더에 `Bearer`로 전송
- **Refresh Token**: SHA-256 해시 후 DB 저장, HttpOnly 쿠키로 전달 (7일)
- `AuthBootstrap` 컴포넌트가 앱 마운트 시 refresh 쿠키로 세션 복원
- 401 응답 시 클라이언트가 자동으로 1회 토큰 갱신 후 재시도; 동시 다발 401은 단일 refresh 요청으로 병합

### ID 생성
- Snowflake ID (`SnowflakeIdGenerator`): 64비트 (타임스탬프 + 데이터센터 + 워커 + 시퀀스)
- JPA `@GeneratedValue` 대신 애플리케이션 레벨에서 생성

### 조건부 인프라
- Redis/Kafka는 `app.redis.enabled`, `app.kafka.enabled` 플래그로 활성화
- 비활성화 시 해당 Bean 미등록 → 테스트 환경에서 외부 인프라 불필요

### 이벤트 기반 인기글
- 게시글 조회 → `board.post.viewed` Kafka 토픽 발행
- Consumer가 Redis 점수를 비동기로 증가
- Redis로 사용자별 중복 조회 방지 (시간 윈도우 내)

### 파일 업로드
- 로컬 디스크(`/data/uploads`)에 저장, S3 마이그레이션을 고려한 추상화 구조
- 파일 메타데이터 DB 저장; 메타데이터 커밋 실패 시 파일 롤백

### CQRS
- 읽기 최적화 쿼리 모델을 쓰기 모델과 분리 (Section 7 구현)

---

## 테스트 전략

### 백엔드
- `application-test.yml`: H2 (MySQL 모드), Flyway 비활성화, Redis/Kafka 비활성화
- Hibernate가 테스트용 스키마 자동 생성
- JWT 테스트 시크릿 별도 설정

### 프론트엔드
- Vitest + jsdom + Testing Library
- 커버리지 대상: `src/shared/**`, `src/features/**`

---

## 환경 변수

### 백엔드 주요 변수

| 변수 | 설명 |
|------|------|
| `APP_JWT_SECRET` | HMAC 서명 키 (최소 32자) |
| `APP_CORS_ALLOWED_ORIGINS` | 쉼표 구분 허용 오리진 |
| `APP_REDIS_ENABLED` | `"true"` 시 Redis 활성화 |
| `APP_KAFKA_ENABLED` | `"true"` 시 Kafka 활성화 |
| `APP_UPLOAD_DIR` | 파일 저장 경로 |
| `APP_SECURITY_BOOTSTRAP_ADMIN` | `"true"` 시 초기 관리자 생성 |

### 프론트엔드 주요 변수

| 변수 | 설명 |
|------|------|
| `VITE_API_PROXY_TARGET` | `/api` 프록시 대상 (기본: `http://localhost:8080`) |
| `VITE_USE_POLLING` | `"true"` 시 파일 폴링 (Docker Desktop용) |

---

## 커밋 메시지 규칙

형식: `type: 설명 (한글)`

| type | 용도 |
|------|------|
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `docs` | 문서 변경 |
| `style` | 코드 포맷 |
| `refactor` | 리팩토링 |
| `test` | 테스트 |
| `chore` | 빌드/설정 |
| `perf` | 성능 개선 |

**올바른 예시:**
```
feat: 게시글 좋아요 기능 추가
fix: 토큰 만료 시 자동 갱신 오류 수정
docs: API 명세 업데이트
refactor: PostService 조회 로직 분리
test: 댓글 작성 단위 테스트 추가
chore: commitlint + husky 커밋 메시지 규칙 설정
perf: 게시글 목록 쿼리 N+1 개선
```

**잘못된 예시:**
```
기능 추가          ← type 없음
feat 로그인 수정   ← 콜론(:) 누락
FEAT: 수정        ← 대문자 type
```

> commitlint + husky로 형식이 맞지 않으면 커밋이 차단됩니다.

---

## 포트

| 서비스 | 포트 |
|--------|------|
| Frontend | 5173 |
| Backend | 8080 |
| MySQL | 3307 (호스트) → 3306 (컨테이너) |
| Redis | 6379 |
| Redpanda (Kafka) | 19092 (외부), 9092 (내부) |
