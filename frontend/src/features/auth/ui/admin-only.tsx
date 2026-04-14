import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'

import { useAuthStore } from '@/shared/store/auth-store'

export function AdminOnly({ children }: { children: React.ReactNode }) {
  const navigate = useNavigate()
  const user = useAuthStore((s) => s.user)
  const bootstrapped = useAuthStore((s) => s.bootstrapped)

  useEffect(() => {
    if (!bootstrapped) {
      return
    }
    if (user?.role !== 'ADMIN') {
      void navigate('/posts', { replace: true })
    }
  }, [bootstrapped, navigate, user?.role])

  if (!bootstrapped) {
    return (
      <p className="px-4 py-8 text-center text-sm text-muted-foreground">
        권한 확인 중…
      </p>
    )
  }
  if (user?.role !== 'ADMIN') {
    return null
  }
  return <>{children}</>
}
