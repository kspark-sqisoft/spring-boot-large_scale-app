import { useEffect } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'

import { useAuthStore } from '@/shared/store/auth-store'

export function RequireAuth({ children }: { children: React.ReactNode }) {
  const navigate = useNavigate()
  const location = useLocation()
  const accessToken = useAuthStore((s) => s.accessToken)
  const bootstrapped = useAuthStore((s) => s.bootstrapped)

  useEffect(() => {
    if (bootstrapped && !accessToken) {
      void navigate('/login', {
        replace: true,
        state: { from: location.pathname + location.search },
      })
    }
  }, [accessToken, bootstrapped, location.pathname, location.search, navigate])

  if (!bootstrapped) {
    return (
      <p className="px-4 py-8 text-center text-sm text-muted-foreground">
        세션 확인 중…
      </p>
    )
  }
  if (!accessToken) {
    return null
  }
  return <>{children}</>
}
