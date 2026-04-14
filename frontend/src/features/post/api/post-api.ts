import {
  deleteJson,
  deleteResource,
  getJson,
  postJson,
  putJson,
} from '@/shared/api/client'

import type {
  CreatePostBody,
  PostDto,
  PostLikeStatusDto,
  PostPageDto,
} from '../model/post.types'

export function fetchPostsPage(page: number, size: number): Promise<PostPageDto> {
  const q = new URLSearchParams({ page: String(page), size: String(size) })
  return getJson<PostPageDto>(`/posts?${q.toString()}`)
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
