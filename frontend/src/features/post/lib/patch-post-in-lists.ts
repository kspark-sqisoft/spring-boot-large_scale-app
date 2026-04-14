import type { InfiniteData } from '@tanstack/react-query'

import type { PostCursorPageDto, PostDto } from '@/features/post/model/post.types'

type ListPatch = Partial<
  Pick<PostDto, 'likeCount' | 'likedByMe' | 'commentCount'>
>

/**
 * 무한 스크롤 목록 캐시에서 특정 게시글 행만 갱신합니다.
 */
export function patchPostInInfiniteLists(
  old: unknown,
  postId: string,
  patch: ListPatch,
): unknown {
  if (!old || typeof old !== 'object' || !('pages' in old)) {
    return old
  }
  const inf = old as InfiniteData<PostCursorPageDto>
  if (!Array.isArray(inf.pages)) {
    return old
  }
  return {
    ...inf,
    pages: inf.pages.map((page) => ({
      ...page,
      content: page.content.map((p) =>
        String(p.id) === String(postId) ? { ...p, ...patch } : p,
      ),
    })),
  }
}
