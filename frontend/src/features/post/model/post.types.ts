export type PostImageDto = {
  id: string
  url: string
}

export type PostDto = {
  id: string
  title: string
  content: string
  createdAt: string
  updatedAt: string
  images: PostImageDto[]
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
