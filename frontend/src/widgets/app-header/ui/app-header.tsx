import { NavLink, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'

import { logoutRequest } from '@/features/auth/api/auth-api'
import { cn } from '@/shared/lib/utils'
import { useAuthStore } from '@/shared/store/auth-store'
import { Button } from '@/shared/ui/button'
import { ThemeToggle } from '@/shared/ui/theme-toggle'

// 네비·로그인 상태별 링크·로그아웃(서버 쿠키 무효 + zustand 클리어)
const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  cn(
    'rounded-md px-2 py-1 text-sm text-muted-foreground transition-colors hover:text-foreground',
    isActive && 'bg-accent font-medium text-foreground',
  )

export function AppHeader() {
  const navigate = useNavigate()
  const accessToken = useAuthStore((s) => s.accessToken)
  const user = useAuthStore((s) => s.user)

  async function onLogout() {
    try {
      await logoutRequest()
      toast.success('로그아웃했습니다.')
      void navigate('/posts')
    } catch {
      toast.error('로그아웃 요청에 실패했습니다.')
    }
  }

  return (
    <header className="border-b bg-card/80 backdrop-blur supports-[backdrop-filter]:bg-card/60">
      <div className="mx-auto flex max-w-2xl flex-wrap items-center justify-between gap-3 px-4 py-3">
        <div className="flex flex-wrap items-center gap-1">
          <span className="mr-2 text-sm font-semibold text-foreground">
            board.dev
          </span>
          <NavLink to="/posts" className={navLinkClass} end>
            게시글
          </NavLink>
          <NavLink to="/posts/popular" className={navLinkClass}>
            인기
          </NavLink>
          {accessToken ? (
            <NavLink to="/posts/new" className={navLinkClass}>
              작성
            </NavLink>
          ) : null}
          <NavLink to="/health" className={navLinkClass}>
            상태
          </NavLink>
          {user?.role === 'ADMIN' ? (
            <NavLink to="/admin/users" className={navLinkClass}>
              관리
            </NavLink>
          ) : null}
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {accessToken ? (
            <>
              <NavLink
                to="/profile"
                className={({ isActive }) =>
                  cn(
                    'flex max-w-[min(220px,45vw)] items-center gap-2 truncate rounded-md px-2 py-1 text-xs transition-colors hover:bg-accent hover:text-foreground',
                    isActive && 'bg-accent font-medium text-foreground',
                  )
                }
                title="프로필"
              >
                {user?.avatarUrl ? (
                  <img
                    src={user.avatarUrl}
                    alt=""
                    className="h-7 w-7 shrink-0 rounded-full object-cover"
                  />
                ) : (
                  <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-muted text-[10px] text-muted-foreground">
                    {(user?.displayName ?? user?.email ?? '?').slice(0, 1).toUpperCase()}
                  </span>
                )}
                <span className="min-w-0 truncate text-muted-foreground">
                  {user?.displayName?.trim() || user?.email || '…'}
                </span>
              </NavLink>
              <Button type="button" variant="outline" size="sm" onClick={() => void onLogout()}>
                로그아웃
              </Button>
            </>
          ) : (
            <>
              <Button type="button" variant="ghost" size="sm" asChild>
                <NavLink to="/login">로그인</NavLink>
              </Button>
              <Button type="button" variant="secondary" size="sm" asChild>
                <NavLink to="/register">가입</NavLink>
              </Button>
            </>
          )}
          <ThemeToggle />
        </div>
      </div>
    </header>
  )
}
