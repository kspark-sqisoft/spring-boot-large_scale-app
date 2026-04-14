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

export type PostPageDto = {
  content: PostDto[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type CreatePostBody = {
  title: string
  content: string
  imageFileIds?: string[]
}
