import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'

import { getJson } from '@/shared/api/client'
import { Button } from '@/shared/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/shared/ui/card'

type UserSummary = {
  id: string
  email: string
  role: string
  createdAt: string
}

type UserPage = {
  content: UserSummary[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export function AdminUsersPage() {
  const { data, isPending, isError, error } = useQuery({
    queryKey: ['admin', 'users', 0],
    queryFn: () => getJson<UserPage>('/admin/users?page=0&size=50'),
  })

  return (
    <div className="space-y-6 px-4 py-6">
      <div className="flex flex-wrap items-center gap-3">
        <Button variant="ghost" size="sm" asChild>
          <Link to="/posts">← 게시글</Link>
        </Button>
        <h1 className="text-2xl font-bold tracking-tight">관리 · 사용자 목록</h1>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>사용자</CardTitle>
          <CardDescription>ADMIN 역할만 접근할 수 있습니다.</CardDescription>
        </CardHeader>
        <CardContent>
          {isPending ? (
            <p className="text-sm text-muted-foreground">불러오는 중…</p>
          ) : null}
          {isError ? (
            <p className="text-sm text-destructive" role="alert">
              {error instanceof Error ? error.message : '조회 실패'}
            </p>
          ) : null}
          {data ? (
            <ul className="divide-y rounded-md border">
              {data.content.map((u) => (
                <li
                  key={u.id}
                  className="flex flex-wrap items-baseline justify-between gap-2 px-3 py-2 text-sm"
                >
                  <span className="font-medium">{u.email}</span>
                  <span className="text-muted-foreground">
                    {u.role} · {u.id}
                  </span>
                </li>
              ))}
            </ul>
          ) : null}
        </CardContent>
      </Card>
    </div>
  )
}
