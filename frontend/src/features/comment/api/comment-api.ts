import {
  deleteResource,
  getJson,
  postJson,
  putJson,
} from '@/shared/api/client'

import type {
  CommentDto,
  CommentListDto,
  CreateCommentBody,
} from '../model/comment.types'

export function fetchComments(postId: string): Promise<CommentListDto> {
  return getJson<CommentListDto>(
    `/posts/${encodeURIComponent(postId)}/comments`,
  )
}

export function createComment(
  postId: string,
  body: CreateCommentBody,
): Promise<CommentDto> {
  return postJson<CommentDto>(
    `/posts/${encodeURIComponent(postId)}/comments`,
    body,
  )
}

export function updateComment(
  postId: string,
  commentId: string,
  content: string,
): Promise<CommentDto> {
  return putJson<CommentDto>(
    `/posts/${encodeURIComponent(postId)}/comments/${encodeURIComponent(commentId)}`,
    { content },
  )
}

export function deleteComment(postId: string, commentId: string): Promise<void> {
  return deleteResource(
    `/posts/${encodeURIComponent(postId)}/comments/${encodeURIComponent(commentId)}`,
  )
}
