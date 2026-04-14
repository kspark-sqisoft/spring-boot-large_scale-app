export type CommentAuthorDto = {
  id: string
  displayName: string
}

export type CommentDto = {
  id: string
  postId: string
  parentCommentId: string | null
  depth: number
  content: string
  author: CommentAuthorDto
  createdAt: string
  updatedAt: string
}

export type CommentListDto = {
  comments: CommentDto[]
}

export type CreateCommentBody = {
  content: string
  parentCommentId?: string | null
}
