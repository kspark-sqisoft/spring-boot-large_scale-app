import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { toast } from 'sonner'

import { PostCommentsSection } from '@/features/comment'
import { deletePost, fetchPost, likePost, unlikePost } from '@/features/post/api/post-api'
import type { PostDto, PostPageDto } from '@/features/post/model/post.types'
import { useAuthStore } from '@/shared/store/auth-store'
import { postKeys } from '@/features/post/api/post-keys'
import { Button } from '@/shared/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/shared/ui/card'

export function PostDetailPage() {
  const { postId } = useParams<{ postId: string }>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const accessToken = useAuthStore((s) => s.accessToken)

  const { data, isPending, isError, error } = useQuery({
    queryKey: postKeys.detail(postId ?? ''),
    queryFn: () => fetchPost(postId!),
    enabled: Boolean(postId),
  })

  const deleteMutation = useMutation({
    mutationFn: () => deletePost(postId!),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: postKeys.lists() })
      void queryClient.removeQueries({ queryKey: postKeys.detail(postId!) })
      toast.success('삭제했습니다.')
      void navigate('/posts')
    },
    onError: (e: Error) => toast.error(e.message),
  })

  const likeMutation = useMutation({
    mutationFn: async () => {
      const current = queryClient.getQueryData<PostDto>(postKeys.detail(postId!))
      if (current?.likedByMe) {
        return unlikePost(postId!)
      }
      return likePost(postId!)
    },
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: postKeys.detail(postId!) })
      const previous = queryClient.getQueryData<PostDto>(postKeys.detail(postId!))
      if (previous) {
        const liked = !previous.likedByMe
        const likeCount = Math.max(0, previous.likeCount + (liked ? 1 : -1))
        const next = { ...previous, likedByMe: liked, likeCount }
        queryClient.setQueryData(postKeys.detail(postId!), next)
        queryClient.setQueriesData({ queryKey: postKeys.lists() }, (old: PostPageDto | undefined) => {
          if (!old?.content?.length) {
            return old
          }
          const idx = old.content.findIndex((p) => p.id === postId)
          if (idx === -1) {
            return old
          }
          const content = [...old.content]
          content[idx] = { ...content[idx], likedByMe: liked, likeCount }
          return { ...old, content }
        })
      }
      return { previous }
    },
    onError: (e: Error, _v, context) => {
      if (context?.previous !== undefined) {
        queryClient.setQueryData(postKeys.detail(postId!), context.previous)
      }
      void queryClient.invalidateQueries({ queryKey: postKeys.lists() })
      toast.error(e.message)
    },
    onSuccess: (status) => {
      queryClient.setQueryData(postKeys.detail(postId!), (prev: unknown) => {
        if (!prev || typeof prev !== 'object') {
          return prev
        }
        const p = prev as PostDto
        return {
          ...p,
          likeCount: status.likeCount,
          likedByMe: status.likedByMe,
        }
      })
      queryClient.setQueriesData({ queryKey: postKeys.lists() }, (old: PostPageDto | undefined) => {
        if (!old?.content?.length) {
          return old
        }
        return {
          ...old,
          content: old.content.map((p) =>
            p.id === postId
              ? { ...p, likeCount: status.likeCount, likedByMe: status.likedByMe }
              : p,
          ),
        }
      })
    },
  })

  if (!postId) {
    return <p className="text-destructive">잘못된 경로입니다.</p>
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center gap-2">
        <Button variant="ghost" size="sm" asChild>
          <Link to="/posts">← 목록</Link>
        </Button>
        {data && accessToken ? (
          <>
            <Button
              type="button"
              variant={data.likedByMe ? 'secondary' : 'outline'}
              size="sm"
              disabled={likeMutation.isPending}
              onClick={() => likeMutation.mutate()}
            >
              {data.likedByMe ? '♥ 좋아요 취소' : '♡ 좋아요'} ({data.likeCount})
            </Button>
            <Button variant="outline" size="sm" asChild>
              <Link to={`/posts/${postId}/edit`}>수정</Link>
            </Button>
            <Button
              type="button"
              variant="destructive"
              size="sm"
              disabled={deleteMutation.isPending}
              onClick={() => {
                if (window.confirm('이 게시글을 삭제할까요?')) {
                  deleteMutation.mutate()
                }
              }}
            >
              {deleteMutation.isPending ? '삭제 중…' : '삭제'}
            </Button>
          </>
        ) : null}
      </div>

      {isPending ? (
        <p className="text-sm text-muted-foreground">불러오는 중…</p>
      ) : null}
      {isError ? (
        <p className="text-sm text-destructive" role="alert">
          {error instanceof Error ? error.message : '불러오기 실패'}
        </p>
      ) : null}

      {data ? (
        <Card>
          <CardHeader>
            <CardTitle className="text-2xl">{data.title}</CardTitle>
            <CardDescription>
              작성 {new Date(data.createdAt).toLocaleString('ko-KR')} · 수정{' '}
              {new Date(data.updatedAt).toLocaleString('ko-KR')}
              {' · '}
              좋아요 {data.likeCount} · 댓글 {data.commentCount}
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            <pre className="whitespace-pre-wrap font-sans text-sm leading-relaxed">
              {data.content}
            </pre>
            {data.images.length > 0 ? (
              <div className="space-y-2">
                <p className="text-sm font-medium">이미지</p>
                <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3">
                  {data.images.map((img) => (
                    <li key={img.id} className="overflow-hidden rounded-md border">
                      <a href={img.url} target="_blank" rel="noreferrer">
                        <img
                          src={img.url}
                          alt=""
                          className="aspect-square w-full object-cover hover:opacity-90"
                        />
                      </a>
                    </li>
                  ))}
                </ul>
              </div>
            ) : null}
          </CardContent>
          <CardFooter className="text-xs text-muted-foreground">
            id: {data.id}
          </CardFooter>
        </Card>
      ) : null}

      {data ? <PostCommentsSection postId={data.id} /> : null}
    </div>
  )
}
