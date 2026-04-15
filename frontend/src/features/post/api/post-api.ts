import {
  deleteJson,
  deleteResource,
  getJson,
  postJson,
  putJson,
} from '@/shared/api/client'

import type {
  CreatePostBody,
  PostCursorPageDto,
  PostDto,
  PostLikeStatusDto,
} from '../model/post.types'

// 게시글 REST 래퍼 — 실제 HTTP는 shared/api/client(getJson·postJson…)가 처리

export function fetchPostsCursorPage(
  size: number,
  cursor: string | null,
): Promise<PostCursorPageDto> {
  const q = new URLSearchParams({ size: String(size) })
  if (cursor) {
    q.set('cursor', cursor)
  }
  return getJson<PostCursorPageDto>(`/posts?${q.toString()}`)
}

export function fetchPopularPosts(limit: number): Promise<PostDto[]> {
  const q = new URLSearchParams({ limit: String(limit) })
  return getJson<PostDto[]>(`/posts/popular?${q.toString()}`)
}

export function fetchPost(id: string): Promise<PostDto> {
  return getJson<PostDto>(`/posts/${encodeURIComponent(id)}`)
}

export function createPost(body: CreatePostBody): Promise<PostDto> {
  return postJson<PostDto>('/posts', body)
}

export function updatePost(id: string, body: CreatePostBody): Promise<PostDto> {
  return putJson<PostDto>(`/posts/${encodeURIComponent(id)}`, body)
}

export function deletePost(id: string): Promise<void> {
  return deleteResource(`/posts/${encodeURIComponent(id)}`)
}

export function likePost(postId: string): Promise<PostLikeStatusDto> {
  return postJson<PostLikeStatusDto>(
    `/posts/${encodeURIComponent(postId)}/likes`,
    {},
  )
}

export function unlikePost(postId: string): Promise<PostLikeStatusDto> {
  return deleteJson<PostLikeStatusDto>(
    `/posts/${encodeURIComponent(postId)}/likes`,
  )
}
