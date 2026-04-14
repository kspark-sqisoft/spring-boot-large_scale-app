import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'

import { registerRequest } from '@/features/auth/api/auth-api'
import { HttpError } from '@/shared/api/client'
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

export function RegisterPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [pending, setPending] = useState(false)

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (password.length < 8) {
      toast.error('비밀번호는 8자 이상이어야 합니다.')
      return
    }
    setPending(true)
    try {
      await registerRequest(email.trim(), password)
      toast.success('가입했습니다.')
      void navigate('/posts', { replace: true })
    } catch (err) {
      const msg =
        err instanceof HttpError
          ? err.message
          : err instanceof Error
            ? err.message
            : '가입에 실패했습니다.'
      toast.error(msg)
    } finally {
      setPending(false)
    }
  }

  return (
    <div className="mx-auto max-w-md space-y-6 px-4 py-8">
      <h1 className="text-2xl font-bold tracking-tight">회원가입</h1>
      <Card>
        <CardHeader>
          <CardTitle>새 계정</CardTitle>
          <CardDescription>비밀번호는 8자 이상으로 설정하세요.</CardDescription>
        </CardHeader>
        <form onSubmit={onSubmit}>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="reg-email">이메일</Label>
              <Input
                id="reg-email"
                type="email"
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="reg-password">비밀번호</Label>
              <Input
                id="reg-password"
                type="password"
                autoComplete="new-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                minLength={8}
              />
            </div>
          </CardContent>
          <CardFooter className="flex flex-col gap-3 sm:flex-row sm:justify-between">
            <Button type="submit" disabled={pending}>
              {pending ? '처리 중…' : '가입하기'}
            </Button>
            <Button type="button" variant="ghost" size="sm" asChild>
              <Link to="/login">로그인</Link>
            </Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  )
}
