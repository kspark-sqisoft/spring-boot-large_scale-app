---
name: frontend-design
description: 이 프로젝트(board.dev 게시판)의 프론트엔드 UI를 작업할 때 적용하는 디자인 시스템 가이드. shadcn/ui + Tailwind CSS v3 기반.
origin: ECC (project-adapted)
---

# Frontend Design — board.dev

이 프로젝트의 UI 작업은 **Editorial/Minimal** 방향으로 한다.
콘텐츠(게시글·댓글)가 주인공이고, UI는 그것을 방해하지 않는다.

## 비주얼 방향

**Editorial/Minimal** — 게시판은 읽기 공간이다.
- 타이포그래피와 여백이 디자인의 80%를 차지한다.
- 장식보다 계층 구조(hierarchy)에 투자한다.
- 다크모드와 라이트모드 모두 동일하게 의도적으로 보여야 한다.

## 디자인 토큰 시스템

### 파일 위치
- CSS 변수: `frontend/src/index.css` (shadcn 호환 HSL 토큰)
- TS 상수: `frontend/src/shared/ui/design-tokens.ts`
- Tailwind 설정: `frontend/tailwind.config.cjs`

### 색상 원칙
- **라이트모드**: 따뜻한 오프화이트 배경(220 20% 97%) + 진한 차콜 텍스트
- **다크모드**: 순수 검정 대신 따뜻한 다크그레이(220 13% 9%)
- **Accent**: Slate 계열 (신뢰감, 가독성)
- 보라색 그라디언트 금지. 강조색은 하나만.

### 타이포그래피 원칙
- 헤딩: `font-semibold`, 타이트한 자간(`tracking-tight`)
- 본문: `leading-relaxed` (line-height 1.625) — 긴 게시글 가독성
- 메타 정보(날짜·작성자): `text-muted-foreground text-sm`
- 폰트 스택: 시스템 폰트 우선, 웹폰트는 선택적 적용

### 여백 리듬
- 기본 단위: 4px (Tailwind 기본)
- 섹션 간격: `py-10` (40px)
- 카드 내부: `p-4` / md: `p-6`
- 리스트 아이템 간격: `gap-3`

### 모션 원칙
- 전환: `transition-colors duration-150` — 색상 변화에만
- 페이지 진입 애니메이션 금지 (빠른 탐색이 중요)
- hover: `hover:bg-accent` 수준의 최소 피드백
- 로딩 스켈레톤은 허용

### 레이아웃 원칙
- 콘텐츠 최대 너비: `max-w-2xl` (672px) — 긴 글 읽기 최적
- 모바일 우선: `px-4` 기본, md 브레이크포인트에서 여백 확장
- 게시글 목록은 카드 그리드 아닌 수직 스택
- 구분선은 `border-b` 하나로 충분 — 불필요한 그림자 없음

## 컴포넌트 작성 규칙

### 새 컴포넌트 추가 시
1. `frontend/src/shared/ui/` — 프로젝트 전역 공용 컴포넌트
2. `frontend/src/features/<feature>/ui/` — 도메인 전용 컴포넌트
3. shadcn/ui 컴포넌트가 있으면 커스텀 구현 전에 먼저 확인

### 금지 패턴
- `text-blue-500`, `text-green-600` 등 하드코딩 색상 — 반드시 토큰 사용
- `style={{}}` 인라인 스타일 — Tailwind 클래스 또는 CSS 변수로
- 의미 없는 hover 애니메이션
- 게시글·댓글·사용자에 동일한 카드 컴포넌트 남용

### 접근성
- 모든 인터랙티브 요소에 `focus-visible:ring` 유지
- 색상만으로 상태 구분 금지
- 아이콘만 있는 버튼에 `aria-label` 필수

## 품질 게이트

UI 작업 완료 전 확인:
- [ ] 라이트/다크 모드 양쪽에서 의도한 대로 보이는가
- [ ] 모바일(375px)에서 레이아웃이 깨지지 않는가
- [ ] 토큰 시스템을 벗어난 하드코딩 색상이 없는가
- [ ] generic AI UI처럼 보이지 않는가 (대칭 카드 더미, 무의미한 그라디언트)
- [ ] 타이포그래피 계층이 스캔 가능한가 (제목 → 부제 → 본문 → 메타)
