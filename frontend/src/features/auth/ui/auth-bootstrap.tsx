import { useEffect, type ReactNode } from 'react'

import { buildApiUrl, getJson } from '@/shared/api/client'
import { useAuthStore } from '@/shared/store/auth-store'

import type { UserMeResponse } from '../model/auth.types'

/** 마운트 시: refresh 쿠키 → 액세스 토큰 저장 → 가능하면 `/users/me`로 user 채움 → bootstrapped=true */
export function AuthBootstrap({ children }: { children: ReactNode }) {
  const setAccessToken = useAuthStore((s) => s.setAccessToken)
  const setUser = useAuthStore((s) => s.setUser)
  const setBootstrapped = useAuthStore((s) => s.setBootstrapped)

  useEffect(() => {
    let cancelled = false
    void (async () => {
      try {
        const res = await fetch(buildApiUrl('/auth/refresh'), {
          method: 'POST',
          credentials: 'include',
        })
        if (cancelled) {
          return
        }
        if (res.ok) {
          const body = (await res.json()) as { accessToken: string }
          setAccessToken(body.accessToken)
          try {
            const me = await getJson<UserMeResponse>('/users/me')
            if (!cancelled) {
              setUser(me)
            }
          } catch {
            /* 토큰만 복구된 상태 */
          }
        }
      } catch {
        /* 쿠키 없음 등 */
      } finally {
        if (!cancelled) {
          setBootstrapped(true)
        }
      }
    })()
    return () => {
      cancelled = true
    }
  }, [setAccessToken, setBootstrapped, setUser])

  return <>{children}</>
}
