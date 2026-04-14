import { useEffect, useLayoutEffect, useMemo, useRef } from 'react'
import { useInfiniteQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'

import { fetchPostsCursorPage } from '@/features/post/api/post-api'
import { postKeys } from '@/features/post/api/post-keys'
import type { PostCursorPageDto } from '@/features/post/model/post.types'
import { Button } from '@/shared/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/shared/ui/card'

/** 한 페이지에 넣는 개수. DB 글 수가 이 값 이하면 한 번에 전부 내려와 «더 보기»가 없음 */
const PAGE_SIZE = 4

export function PostListPage() {
  const lastRowRef = useRef<HTMLLIElement | null>(null)
  const isFetchingNextRef = useRef(false)
  const fetchNextRef = useRef<() => Promise<unknown>>(() => Promise.resolve())

  const { data, isPending, isError, error, fetchNextPage, isFetchingNextPage } =
    useInfiniteQuery({
      queryKey: postKeys.infiniteList(PAGE_SIZE),
      queryFn: ({ pageParam }) =>
        fetchPostsCursorPage(PAGE_SIZE, pageParam as string | null),
      initialPageParam: null as string | null,
      /** 목록은 자주 바뀌므로 캐시를 오래 신선으로 두지 않음(다른 탭·외부 등록 후에도 재방문 시 최신 반영) */
      staleTime: 0,
      refetchOnMount: 'always',
      getNextPageParam: (lastPage: PostCursorPageDto) => {
        if (!lastPage.hasNext || lastPage.nextCursor == null || lastPage.nextCursor === '') {
          return undefined
        }
        return lastPage.nextCursor
      },
    })

  const rows = data?.pages.flatMap((p) => p.content) ?? []

  const lastPage = data?.pages.at(-1)
  const canLoadMore = lastPage?.hasNext === true

  useLayoutEffect(() => {
    isFetchingNextRef.current = isFetchingNextPage
    fetchNextRef.current = fetchNextPage
  }, [isFetchingNextPage, fetchNextPage])

  useEffect(() => {
    const el = lastRowRef.current
    if (!el || !canLoadMore) {
      return
    }

    const io = new IntersectionObserver(
      (entries) => {
        const hit = entries.some((e) => e.isIntersecting)
        if (!hit || isFetchingNextRef.current) {
          return
        }
        void fetchNextRef.current()
      },
      {
        root: null,
        rootMargin: '400px 0px',
        threshold: 0,
      },
    )

    io.observe(el)

    return () => io.disconnect()
  }, [canLoadMore, rows.length, isPending])

  const showEndHint = useMemo(
    () => !isPending && rows.length > 0 && !canLoadMore,
    [isPending, rows.length, canLoadMore],
  )

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

      {!isPending && rows.length === 0 ? (
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

      {rows.length > 0 ? (
        <ul className="space-y-3">
          {rows.map((post, index) => (
            <li
              key={post.id}
              ref={index === rows.length - 1 ? lastRowRef : undefined}
            >
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

      {canLoadMore ? (
        <div className="flex flex-col items-center gap-3 py-2">
          {isFetchingNextPage ? (
            <p className="text-sm text-muted-foreground">더 불러오는 중…</p>
          ) : (
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => void fetchNextPage()}
              disabled={isFetchingNextPage}
            >
              더 보기
            </Button>
          )}
        </div>
      ) : null}

      {showEndHint ? (
        <p className="text-center text-xs text-muted-foreground">
          추가로 불러올 게시글이 없습니다. (한 번에 {PAGE_SIZE}개씩 표시)
        </p>
      ) : null}
    </div>
  )
}
