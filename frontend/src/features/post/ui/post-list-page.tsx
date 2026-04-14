import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'

import { fetchPostsPage } from '@/features/post/api/post-api'
import { postKeys } from '@/features/post/api/post-keys'
import { Button } from '@/shared/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/shared/ui/card'

const PAGE_SIZE = 10

export function PostListPage() {
  const [page, setPage] = useState(0)
  const { data, isPending, isError, error } = useQuery({
    queryKey: postKeys.list(page, PAGE_SIZE),
    queryFn: () => fetchPostsPage(page, PAGE_SIZE),
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-4">
        <h1 className="text-2xl font-bold tracking-tight">게시글</h1>
        <Button asChild>
          <Link to="/posts/new">새 글</Link>
        </Button>
      </div>

      {isPending ? (
        <p className="text-sm text-muted-foreground">불러오는 중…</p>
      ) : null}
      {isError ? (
        <p className="text-sm text-destructive" role="alert">
          {error instanceof Error ? error.message : '목록을 불러오지 못했습니다.'}
        </p>
      ) : null}

      {data && data.content.length === 0 ? (
        <Card>
          <CardHeader>
            <CardTitle>아직 글이 없습니다</CardTitle>
            <CardDescription>첫 게시글을 작성해 보세요.</CardDescription>
          </CardHeader>
          <CardContent>
            <Button asChild>
              <Link to="/posts/new">작성하기</Link>
            </Button>
          </CardContent>
        </Card>
      ) : null}

      {data && data.content.length > 0 ? (
        <ul className="space-y-3">
          {data.content.map((post) => (
            <li key={post.id}>
              <Card className="transition-colors hover:bg-muted/40">
                <CardHeader className="py-4">
                  <Link
                    to={`/posts/${post.id}`}
                    className="block space-y-1 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  >
                    <CardTitle className="text-lg">{post.title}</CardTitle>
                    <CardDescription className="line-clamp-2">
                      {post.content}
                    </CardDescription>
                    <p className="text-xs text-muted-foreground">
                      {new Date(post.createdAt).toLocaleString('ko-KR')}
                      {' · '}
                      좋아요 {post.likeCount} · 댓글 {post.commentCount} · 조회{' '}
                      {post.viewCount}
                    </p>
                  </Link>
                </CardHeader>
              </Card>
            </li>
          ))}
        </ul>
      ) : null}

      {data && data.totalPages > 1 ? (
        <div className="flex items-center justify-center gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={page <= 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            이전
          </Button>
          <span className="text-sm text-muted-foreground">
            {page + 1} / {data.totalPages}
          </span>
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={page >= data.totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
          >
            다음
          </Button>
        </div>
      ) : null}
    </div>
  )
}
