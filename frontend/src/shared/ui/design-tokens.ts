/**
 * design-tokens.ts
 *
 * board.dev 디자인 시스템 상수.
 * CSS 변수(index.css)와 1:1 대응하며 컴포넌트에서 Tailwind 클래스 대신
 * 직접 참조가 필요한 경우(동적 스타일, 테스트 등)에 사용한다.
 *
 * 원칙: Editorial/Minimal — 콘텐츠 우선, 장식 최소
 */

/** 콘텐츠 최대 너비 — 긴 글 읽기 최적 (max-w-2xl) */
export const CONTENT_MAX_WIDTH = '42rem' // 672px

/** 여백 리듬 */
export const spacing = {
  /** 페이지 좌우 패딩 */
  pagePx: 'px-4',
  /** 섹션 상하 패딩 */
  sectionPy: 'py-10',
  /** 카드 내부 패딩 */
  cardPadding: 'p-4 md:p-6',
  /** 리스트 아이템 간격 */
  listGap: 'gap-3',
} as const

/** 타이포그래피 클래스 조합 */
export const typography = {
  /** 페이지/게시글 제목 */
  pageTitle: 'text-xl font-semibold tracking-tight',
  /** 섹션 제목 */
  sectionTitle: 'text-base font-semibold tracking-tight',
  /** 본문 — leading-relaxed로 긴 글 가독성 확보 */
  body: 'text-sm leading-relaxed',
  /** 메타 정보 (날짜, 작성자, 조회수) */
  meta: 'text-xs text-muted-foreground',
  /** 링크 */
  link: 'hover:underline underline-offset-4',
} as const

/** 모션 — 색상 전환에만 적용, 페이지 진입 애니메이션 없음 */
export const motion = {
  colors: 'transition-colors duration-150',
} as const

/** 라운드 — editorial은 날카로운 편 (--radius: 0.375rem) */
export const radius = {
  base: 'rounded-md',
  sm: 'rounded',
  lg: 'rounded-lg',
} as const
