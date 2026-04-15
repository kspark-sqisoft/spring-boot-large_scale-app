import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'

import { fetchPopularPosts } from '@/features/post/api/post-api'
import { postKeys } from '@/features/post/api/post-keys'
import { Card, CardDescription, CardHeader, CardTitle } from '@/shared/ui/card'

const LIMIT = 20

// 백엔드 인기 순위( Redis 집계 등 ) — 환경에 따라 빈 목록일 수 있음
export function PostPopularPage() {
  const { data, isPending, isError, error } = useQuery({
    queryKey: postKeys.popular(LIMIT),
    queryFn: () => fetchPopularPosts(LIMIT),
  })

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold tracking-tight">인기 글</h1>
        <p className="text-sm text-muted-foreground">
          조회 이벤트가 Kafka·Redis로 집계된 순위입니다. Redis/Kafka가 꺼져 있으면 목록이 비어 있을 수
          있습니다.
        </p>
      </div>

      {isPending ? (
        <p className="text-sm text-muted-foreground">불러오는 중…</p>
      ) : null}
      {isError ? (
        <p className="text-sm text-destructive" role="alert">
          {error instanceof Error ? error.message : '목록을 불러오지 못했습니다.'}
        </p>
      ) : null}

      {data && data.length === 0 ? (
        <Card>
          <CardHeader>
            <CardTitle>아직 순위 데이터가 없습니다</CardTitle>
            <CardDescription>
              게시글을 여러 번 열어보면(조회수 이벤트) 순위가 쌓입니다. dev compose에서는 Redis·Kafka가
              켜져 있어야 합니다.
            </CardDescription>
          </CardHeader>
        </Card>
      ) : null}

      {data && data.length > 0 ? (
        <ul className="space-y-3">
          {data.map((post) => (
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
                      좋아요 {post.likeCount} · 댓글 {post.commentCount} · 조회 {post.viewCount}
                    </p>
                  </Link>
                </CardHeader>
              </Card>
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  )
}
