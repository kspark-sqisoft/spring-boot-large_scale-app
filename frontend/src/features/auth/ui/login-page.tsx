import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'

import { loginRequest } from '@/features/auth/api/auth-api'
import { Button } from '@/shared/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/shared/ui/card'
import { Input } from '@/shared/ui/input'
import { Label } from '@/shared/ui/label'
import { HttpError } from '@/shared/api/client'

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const state = location.state as { from?: string } | undefined
  const from = typeof state?.from === 'string' ? state.from : '/posts'

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [pending, setPending] = useState(false)

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    setPending(true)
    try {
      await loginRequest(email.trim(), password)
      toast.success('로그인했습니다.')
      void navigate(from, { replace: true })
    } catch (err) {
      const msg =
        err instanceof HttpError
          ? err.message
          : err instanceof Error
            ? err.message
            : '로그인에 실패했습니다.'
      toast.error(msg)
    } finally {
      setPending(false)
    }
  }

  return (
    <div className="mx-auto max-w-md space-y-6 px-4 py-8">
      <h1 className="text-2xl font-bold tracking-tight">로그인</h1>
      <Card>
        <CardHeader>
          <CardTitle>계정</CardTitle>
          <CardDescription>이메일과 비밀번호를 입력하세요.</CardDescription>
        </CardHeader>
        <form onSubmit={onSubmit}>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="login-email">이메일</Label>
              <Input
                id="login-email"
                type="email"
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="login-password">비밀번호</Label>
              <Input
                id="login-password"
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>
          </CardContent>
          <CardFooter className="flex flex-col gap-3 sm:flex-row sm:justify-between">
            <Button type="submit" disabled={pending}>
              {pending ? '처리 중…' : '로그인'}
            </Button>
            <Button type="button" variant="ghost" size="sm" asChild>
              <Link to="/register">회원가입</Link>
            </Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  )
}
