export type PostImageDto = {
  id: string
  url: string
}

export type PostDto = {
  id: string
  /** 작성자 user id (문자열). 레거시·마이그레이션 전 글은 null */
  authorUserId: string | null
  title: string
  content: string
  createdAt: string
  updatedAt: string
  images: PostImageDto[]
  likeCount: number
  commentCount: number
  likedByMe: boolean
  viewCount: number
}

export type PostLikeStatusDto = {
  likeCount: number
  likedByMe: boolean
}

/** GET /posts 커서 페이지 (무한 스크롤) */
export type PostCursorPageDto = {
  content: PostDto[]
  nextCursor: string | null
  size: number
  /** 서버 기준 다음 페이지 존재 여부(클라이언트는 이 값으로 더보기 노출) */
  hasNext: boolean
}

export type CreatePostBody = {
  title: string
  content: string
  imageFileIds?: string[]
}
