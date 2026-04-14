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
  const user = useAuthStore((s) => s.user)

  const { data, isPending, isError, error } = useQuery({
    queryKey: postKeys.detail(postId ?? ''),
    queryFn: () => fetchPost(postId!),
    enabled: Boolean(postId),
    // 좋아요 낙관적 업데이트 직후 포커스/백그라운드 refetch가 이전 응답으로 덮어쓰지 않도록
    refetchOnWindowFocus: false,
  })

  const canEditDelete = Boolean(
    accessToken &&
      user &&
      data &&
      (user.role === 'ADMIN' ||
        data.authorUserId == null ||
        data.authorUserId === user.id),
  )

  const deleteMutation = useMutation({
    mutationFn: () => {
      const row = queryClient.getQueryData<PostDto>(postKeys.detail(postId!))
      return deletePost(row?.id ?? postId!)
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: postKeys.lists() })
      void queryClient.removeQueries({ queryKey: postKeys.detail(postId!) })
      toast.success('삭제했습니다.')
      void navigate('/posts')
    },
    onError: (e: Error) => toast.error(e.message),
  })

  const likeMutation = useMutation({
    // onMutate가 먼저 돌아 캐시가 뒤집힌 뒤 mutationFn이 실행되므로, API 종류는 변수로 넘깁니다.
    mutationFn: async (likedAfterClick: boolean) => {
      const current = queryClient.getQueryData<PostDto>(postKeys.detail(postId!))
      const pid = current?.id ?? postId!
      return likedAfterClick ? likePost(pid) : unlikePost(pid)
    },
    onMutate: async (likedAfterClick: boolean) => {
      await queryClient.cancelQueries({ queryKey: postKeys.detail(postId!) })
      const previous = queryClient.getQueryData<PostDto>(postKeys.detail(postId!))
      if (previous) {
        const prevCount = Number(previous.likeCount)
        let likeCount = prevCount
        if (likedAfterClick && !previous.likedByMe) {
          likeCount = prevCount + 1
        } else if (!likedAfterClick && previous.likedByMe) {
          likeCount = Math.max(0, prevCount - 1)
        }
        const next = { ...previous, likedByMe: likedAfterClick, likeCount }
        queryClient.setQueryData(postKeys.detail(postId!), next)
        queryClient.setQueriesData({ queryKey: postKeys.lists() }, (old: PostPageDto | undefined) => {
          if (!old?.content?.length) {
            return old
          }
          const idx = old.content.findIndex((p) => String(p.id) === String(postId))
          if (idx === -1) {
            return old
          }
          const content = [...old.content]
          content[idx] = { ...content[idx], likedByMe: likedAfterClick, likeCount }
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
    onSuccess: async (status) => {
      const count = Number(status.likeCount)
      const liked = Boolean(status.likedByMe)
      queryClient.setQueryData(postKeys.detail(postId!), (prev: unknown) => {
        if (!prev || typeof prev !== 'object') {
          return prev
        }
        const p = prev as PostDto
        return {
          ...p,
          likeCount: count,
          likedByMe: liked,
        }
      })
      queryClient.setQueriesData({ queryKey: postKeys.lists() }, (old: PostPageDto | undefined) => {
        if (!old?.content?.length) {
          return old
        }
        return {
          ...old,
          content: old.content.map((p) =>
            String(p.id) === String(postId)
              ? { ...p, likeCount: count, likedByMe: liked }
              : p,
          ),
        }
      })
      // POST 응답이 집계의 근거. 직후 GET refetch는 캐시/프록시·스냅샷 타이밍으로 이전 값을 덮을 수 있음
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
          <Button
            type="button"
            variant={data.likedByMe ? 'secondary' : 'outline'}
            size="sm"
            disabled={likeMutation.isPending}
            onClick={() => likeMutation.mutate(!data.likedByMe)}
          >
            {data.likedByMe ? '♥ 좋아요 취소' : '♡ 좋아요'} ({Number(data.likeCount)})
          </Button>
        ) : null}
        {data && canEditDelete ? (
          <>
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
              좋아요 {Number(data.likeCount)} · 댓글 {data.commentCount} · 조회 {data.viewCount}
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

      {data && postId ? <PostCommentsSection postId={postId} /> : null}
    </div>
  )
}
