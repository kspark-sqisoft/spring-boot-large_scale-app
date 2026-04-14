import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { toast } from 'sonner'

import {
  createComment,
  deleteComment,
  fetchComments,
  updateComment,
} from '@/features/comment/api/comment-api'
import { commentKeys } from '@/features/comment/api/comment-keys'
import { postKeys } from '@/features/post/api/post-keys'
import type { CommentDto, CommentListDto } from '@/features/comment/model/comment.types'
import { patchPostInInfiniteLists } from '@/features/post/lib/patch-post-in-lists'
import type { PostDto } from '@/features/post/model/post.types'
import { useAuthStore } from '@/shared/store/auth-store'
import { Button } from '@/shared/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/shared/ui/card'
import { Label } from '@/shared/ui/label'

function collectRemoveIds(comments: CommentDto[], id: string): Set<string> {
  const remove = new Set<string>([id])
  const target = comments.find((c) => c.id === id)
  if (target && target.depth === 0) {
    for (const c of comments) {
      if (c.parentCommentId === id) {
        remove.add(c.id)
      }
    }
  }
  return remove
}

export function PostCommentsSection({ postId }: { postId: string }) {
  const queryClient = useQueryClient()
  const accessToken = useAuthStore((s) => s.accessToken)
  const user = useAuthStore((s) => s.user)

  const [rootDraft, setRootDraft] = useState('')
  const [replyParentId, setReplyParentId] = useState<string | null>(null)
  const [replyDraft, setReplyDraft] = useState('')
  const [editingId, setEditingId] = useState<string | null>(null)
  const [editDraft, setEditDraft] = useState('')

  const { data, isPending } = useQuery({
    queryKey: commentKeys.post(postId),
    queryFn: () => fetchComments(postId),
    enabled: Boolean(postId),
  })

  const comments = data?.comments ?? []

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: commentKeys.post(postId) })
    void queryClient.invalidateQueries({ queryKey: postKeys.detail(postId) })
    void queryClient.invalidateQueries({ queryKey: postKeys.lists() })
  }

  const createMut = useMutation({
    mutationFn: (body: { content: string; parentCommentId?: string | null }) =>
      createComment(postId, body),
    onMutate: async (body) => {
      await queryClient.cancelQueries({ queryKey: commentKeys.post(postId) })
      const prevComments = queryClient.getQueryData<CommentListDto>(commentKeys.post(postId))
      const prevPost = queryClient.getQueryData<PostDto>(postKeys.detail(postId))
      const prevLists = queryClient.getQueriesData({ queryKey: postKeys.lists() })
      if (!user) {
        return { prevComments, prevPost, prevLists, tempId: '' as const }
      }
      const tempId = `optimistic-${Date.now()}`
      const optimistic: CommentDto = {
        id: tempId,
        postId,
        parentCommentId: body.parentCommentId ?? null,
        depth: body.parentCommentId ? 1 : 0,
        content: body.content,
        author: {
          id: user.id,
          displayName: user.displayName?.trim() || user.email,
        },
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }
      queryClient.setQueryData<CommentListDto>(commentKeys.post(postId), {
        comments: [...(prevComments?.comments ?? []), optimistic],
      })
      if (prevPost) {
        queryClient.setQueryData<PostDto>(postKeys.detail(postId), {
          ...prevPost,
          commentCount: prevPost.commentCount + 1,
        })
      }
      queryClient.setQueriesData({ queryKey: postKeys.lists() }, (old) => {
        const n = queryClient.getQueryData<PostDto>(postKeys.detail(postId))?.commentCount
        if (n === undefined) {
          return old
        }
        return patchPostInInfiniteLists(old, postId, { commentCount: n })
      })
      return { prevComments, prevPost, prevLists, tempId }
    },
    onError: (e: Error, _body, ctx) => {
      if (ctx?.prevComments !== undefined) {
        queryClient.setQueryData(commentKeys.post(postId), ctx.prevComments)
      }
      if (ctx?.prevPost !== undefined) {
        queryClient.setQueryData(postKeys.detail(postId), ctx.prevPost)
      }
      if (ctx?.prevLists) {
        for (const [key, data] of ctx.prevLists) {
          queryClient.setQueryData(key, data)
        }
      }
      void queryClient.invalidateQueries({ queryKey: postKeys.lists() })
      toast.error(e.message)
    },
    onSuccess: (server, _body, ctx) => {
      if (!ctx?.tempId) {
        invalidate()
        setRootDraft('')
        setReplyDraft('')
        setReplyParentId(null)
        toast.success('댓글을 등록했습니다.')
        return
      }
      queryClient.setQueryData<CommentListDto>(commentKeys.post(postId), (old) => {
        const list = old?.comments ?? []
        const next = list.map((c) => (c.id === ctx.tempId ? server : c))
        next.sort(
          (a, b) =>
            new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime(),
        )
        return { comments: next }
      })
      setRootDraft('')
      setReplyDraft('')
      setReplyParentId(null)
      toast.success('댓글을 등록했습니다.')
    },
  })

  const updateMut = useMutation({
    mutationFn: ({ id, content }: { id: string; content: string }) =>
      updateComment(postId, id, content),
    onMutate: async ({ id, content }) => {
      await queryClient.cancelQueries({ queryKey: commentKeys.post(postId) })
      const prevComments = queryClient.getQueryData<CommentListDto>(commentKeys.post(postId))
      if (prevComments) {
        const now = new Date().toISOString()
        queryClient.setQueryData<CommentListDto>(commentKeys.post(postId), {
          comments: prevComments.comments.map((c) =>
            c.id === id ? { ...c, content, updatedAt: now } : c,
          ),
        })
      }
      return { prevComments }
    },
    onError: (e: Error, _v, ctx) => {
      if (ctx?.prevComments !== undefined) {
        queryClient.setQueryData(commentKeys.post(postId), ctx.prevComments)
      }
      toast.error(e.message)
    },
    onSuccess: (server, { id }) => {
      queryClient.setQueryData<CommentListDto>(commentKeys.post(postId), (old) => {
        if (!old) {
          return old
        }
        return {
          comments: old.comments.map((c) => (c.id === id ? server : c)),
        }
      })
      setEditingId(null)
      toast.success('수정했습니다.')
    },
  })

  const deleteMut = useMutation({
    mutationFn: (id: string) => deleteComment(postId, id),
    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey: commentKeys.post(postId) })
      const prevComments = queryClient.getQueryData<CommentListDto>(commentKeys.post(postId))
      const prevPost = queryClient.getQueryData<PostDto>(postKeys.detail(postId))
      const prevLists = queryClient.getQueriesData({ queryKey: postKeys.lists() })
      if (!prevComments) {
        return { prevComments, prevPost, prevLists }
      }
      const removeIds = collectRemoveIds(prevComments.comments, id)
      queryClient.setQueryData<CommentListDto>(commentKeys.post(postId), {
        comments: prevComments.comments.filter((c) => !removeIds.has(c.id)),
      })
      const removed = removeIds.size
      let nextCommentCount: number | undefined
      if (prevPost) {
        nextCommentCount = Math.max(0, prevPost.commentCount - removed)
        queryClient.setQueryData<PostDto>(postKeys.detail(postId), {
          ...prevPost,
          commentCount: nextCommentCount,
        })
      }
      if (nextCommentCount !== undefined) {
        queryClient.setQueriesData({ queryKey: postKeys.lists() }, (old) =>
          patchPostInInfiniteLists(old, postId, { commentCount: nextCommentCount }),
        )
      }
      return { prevComments, prevPost, prevLists }
    },
    onError: (e: Error, _id, ctx) => {
      if (ctx?.prevComments !== undefined) {
        queryClient.setQueryData(commentKeys.post(postId), ctx.prevComments)
      }
      if (ctx?.prevPost !== undefined) {
        queryClient.setQueryData(postKeys.detail(postId), ctx.prevPost)
      }
      if (ctx?.prevLists) {
        for (const [key, data] of ctx.prevLists) {
          queryClient.setQueryData(key, data)
        }
      }
      void queryClient.invalidateQueries({ queryKey: postKeys.lists() })
      toast.error(e.message)
    },
    onSuccess: () => {
      toast.success('삭제했습니다.')
    },
  })

  const isMine = (c: CommentDto) => user?.id === c.author.id

  return (
    <Card>
      <CardHeader>
        <CardTitle>댓글</CardTitle>
        <CardDescription>
          최대 2단계(본 댓글 · 답글)까지 작성할 수 있습니다.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {isPending ? (
          <p className="text-sm text-muted-foreground">댓글 불러오는 중…</p>
        ) : null}

        <ul className="space-y-4">
          {comments.map((c) => (
            <li
              key={c.id}
              className={c.depth > 0 ? 'border-l-2 border-muted pl-4' : ''}
            >
              <div className="flex flex-wrap items-baseline justify-between gap-2">
                <span className="text-sm font-medium">{c.author.displayName}</span>
                <span className="text-xs text-muted-foreground">
                  {new Date(c.createdAt).toLocaleString('ko-KR')}
                </span>
              </div>
              {editingId === c.id ? (
                <div className="mt-2 space-y-2">
                  <textarea
                    value={editDraft}
                    onChange={(e) => setEditDraft(e.target.value)}
                    rows={3}
                    maxLength={2000}
                    className="flex min-h-[72px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  />
                  <div className="flex gap-2">
                    <Button
                      type="button"
                      size="sm"
                      disabled={updateMut.isPending || !editDraft.trim()}
                      onClick={() =>
                        updateMut.mutate({ id: c.id, content: editDraft.trim() })
                      }
                    >
                      저장
                    </Button>
                    <Button
                      type="button"
                      size="sm"
                      variant="outline"
                      onClick={() => setEditingId(null)}
                    >
                      취소
                    </Button>
                  </div>
                </div>
              ) : (
                <pre className="mt-1 whitespace-pre-wrap font-sans text-sm text-foreground">
                  {c.content}
                </pre>
              )}

              <div className="mt-2 flex flex-wrap gap-2">
                {accessToken && c.depth === 0 ? (
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="h-7 text-xs"
                    onClick={() => {
                      setReplyParentId((prev) => (prev === c.id ? null : c.id))
                      setReplyDraft('')
                    }}
                  >
                    {replyParentId === c.id ? '답글 취소' : '답글'}
                  </Button>
                ) : null}
                {accessToken && isMine(c) && editingId !== c.id ? (
                  <>
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="h-7 text-xs"
                      onClick={() => {
                        setEditingId(c.id)
                        setEditDraft(c.content)
                      }}
                    >
                      수정
                    </Button>
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="h-7 text-xs text-destructive"
                      disabled={deleteMut.isPending}
                      onClick={() => {
                        if (window.confirm('이 댓글을 삭제할까요?')) {
                          deleteMut.mutate(c.id)
                        }
                      }}
                    >
                      삭제
                    </Button>
                  </>
                ) : null}
              </div>

              {replyParentId === c.id && accessToken ? (
                <div className="mt-3 space-y-2 rounded-md border border-dashed p-3">
                  <Label htmlFor={`reply-${c.id}`}>답글 작성</Label>
                  <textarea
                    id={`reply-${c.id}`}
                    value={replyDraft}
                    onChange={(e) => setReplyDraft(e.target.value)}
                    rows={3}
                    maxLength={2000}
                    placeholder="답글 내용"
                    className="flex min-h-[72px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  />
                  <Button
                    type="button"
                    size="sm"
                    disabled={createMut.isPending || !replyDraft.trim()}
                    onClick={() =>
                      createMut.mutate({
                        content: replyDraft.trim(),
                        parentCommentId: c.id,
                      })
                    }
                  >
                    답글 등록
                  </Button>
                </div>
              ) : null}
            </li>
          ))}
        </ul>

        {accessToken ? (
          <div className="space-y-2 border-t pt-4">
            <Label htmlFor="root-comment">새 댓글</Label>
            <textarea
              id="root-comment"
              value={rootDraft}
              onChange={(e) => setRootDraft(e.target.value)}
              rows={4}
              maxLength={2000}
              placeholder="댓글을 입력하세요"
              className="flex min-h-[96px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            />
            <Button
              type="button"
              disabled={createMut.isPending || !rootDraft.trim()}
              onClick={() => createMut.mutate({ content: rootDraft.trim() })}
            >
              댓글 등록
            </Button>
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">
            댓글을 쓰려면 로그인하세요.
          </p>
        )}
      </CardContent>
    </Card>
  )
}
