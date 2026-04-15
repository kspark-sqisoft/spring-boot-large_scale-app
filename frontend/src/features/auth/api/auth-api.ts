import { buildApiUrl, HttpError } from '@/shared/api/client'
import { getJson } from '@/shared/api/client'
import { useAuthStore } from '@/shared/store/auth-store'

import type { AuthSessionResponse, UserMeResponse } from '../model/auth.types'

// 로그인/가입은 공용 client를 쓰지 않고 직접 fetch(무한 refresh 방지·쿠키 Set-Cookie 수신)

async function parseHttpError(res: Response): Promise<HttpError> {
  try {
    const j = (await res.json()) as { message?: string; code?: string }
    return new HttpError(
      res.status,
      j.message ?? `HTTP ${res.status}`,
      j.code,
    )
  } catch {
    return new HttpError(res.status, `HTTP ${res.status}`)
  }
}

export async function loginRequest(
  email: string,
  password: string,
): Promise<AuthSessionResponse> {
  const res = await fetch(buildApiUrl('/auth/login'), {
    method: 'POST',
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ email, password }),
  })
  if (!res.ok) {
    throw await parseHttpError(res)
  }
  const data = (await res.json()) as AuthSessionResponse
  useAuthStore.getState().setSession(data.accessToken, data.user)
  return data
}

export async function registerRequest(
  email: string,
  password: string,
): Promise<AuthSessionResponse> {
  const res = await fetch(buildApiUrl('/auth/register'), {
    method: 'POST',
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ email, password }),
  })
  if (!res.ok) {
    throw await parseHttpError(res)
  }
  const data = (await res.json()) as AuthSessionResponse
  useAuthStore.getState().setSession(data.accessToken, data.user)
  return data
}

export async function logoutRequest(): Promise<void> {
  const token = useAuthStore.getState().accessToken
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  try {
    await fetch(buildApiUrl('/auth/logout'), {
      method: 'POST',
      credentials: 'include',
      headers,
    })
  } finally {
    useAuthStore.getState().clearSession()
  }
}

export function fetchCurrentUser(): Promise<UserMeResponse> {
  return getJson<UserMeResponse>('/users/me')
}
