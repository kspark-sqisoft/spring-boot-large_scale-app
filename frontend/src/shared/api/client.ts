import { useAuthStore } from '@/shared/store/auth-store'

const API_V1 = '/api/v1'

export function buildApiUrl(path: string): string {
  if (path.startsWith('http')) {
    return path
  }
  const p = path.startsWith('/') ? path : `/${path}`
  return `${API_V1}${p}`
}

export class HttpError extends Error {
  readonly status: number
  readonly code?: string

  constructor(status: number, message: string, code?: string) {
    super(message)
    this.name = 'HttpError'
    this.status = status
    this.code = code
  }
}

let refreshPromise: Promise<boolean> | null = null

function refreshAccessToken(): Promise<boolean> {
  if (refreshPromise) {
    return refreshPromise
  }
  const p = (async () => {
    try {
      const res = await fetch(buildApiUrl('/auth/refresh'), {
        method: 'POST',
        credentials: 'include',
      })
      if (!res.ok) {
        return false
      }
      const data = (await res.json()) as { accessToken: string }
      useAuthStore.getState().setAccessToken(data.accessToken)
      return true
    } catch {
      return false
    } finally {
      refreshPromise = null
    }
  })()
  refreshPromise = p
  return p
}

function isNoBearerPath(path: string): boolean {
  return (
    path.startsWith('/auth/login') ||
    path.startsWith('/auth/register') ||
    path.startsWith('/auth/refresh')
  )
}

async function apiRequest(
  path: string,
  init: RequestInit = {},
  retried = false,
): Promise<Response> {
  const headers = new Headers(init.headers)
  const token = useAuthStore.getState().accessToken
  if (token && !isNoBearerPath(path)) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  const res = await fetch(buildApiUrl(path), {
    ...init,
    cache: init.cache ?? 'no-store',
    headers,
    credentials: 'include',
  })
  if (
    res.status === 401 &&
    !retried &&
    !isNoBearerPath(path) &&
    !path.startsWith('/auth/logout')
  ) {
    const refreshed = await refreshAccessToken()
    if (refreshed) {
      return apiRequest(path, init, true)
    }
    useAuthStore.getState().clearSession()
  }
  return res
}

async function toHttpError(res: Response): Promise<HttpError> {
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

async function ensureOk(res: Response): Promise<void> {
  if (res.ok) {
    return
  }
  throw await toHttpError(res)
}

export async function getJson<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await apiRequest(path, {
    ...init,
    method: init?.method ?? 'GET',
    headers: {
      Accept: 'application/json',
      ...init?.headers,
    },
  })
  await ensureOk(res)
  return res.json() as Promise<T>
}

export async function postJson<TResponse>(
  path: string,
  payload: unknown,
  init?: Omit<RequestInit, 'body' | 'method'>,
): Promise<TResponse> {
  const res = await apiRequest(path, {
    ...init,
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      ...init?.headers,
    },
    body: JSON.stringify(payload),
  })
  await ensureOk(res)
  return res.json() as Promise<TResponse>
}

export async function putJson<TResponse>(
  path: string,
  payload: unknown,
  init?: Omit<RequestInit, 'body' | 'method'>,
): Promise<TResponse> {
  const res = await apiRequest(path, {
    ...init,
    method: 'PUT',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      ...init?.headers,
    },
    body: JSON.stringify(payload),
  })
  await ensureOk(res)
  return res.json() as Promise<TResponse>
}

export async function deleteResource(
  path: string,
  init?: RequestInit,
): Promise<void> {
  const res = await apiRequest(path, {
    ...init,
    method: 'DELETE',
    headers: {
      Accept: 'application/json',
      ...init?.headers,
    },
  })
  await ensureOk(res)
}

export async function deleteJson<TResponse>(
  path: string,
  init?: RequestInit,
): Promise<TResponse> {
  const res = await apiRequest(path, {
    ...init,
    method: 'DELETE',
    headers: {
      Accept: 'application/json',
      ...init?.headers,
    },
  })
  await ensureOk(res)
  return res.json() as Promise<TResponse>
}

export async function patchJson<TResponse>(
  path: string,
  payload: unknown,
  init?: Omit<RequestInit, 'body' | 'method'>,
): Promise<TResponse> {
  const res = await apiRequest(path, {
    ...init,
    method: 'PATCH',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      ...init?.headers,
    },
    body: JSON.stringify(payload),
  })
  await ensureOk(res)
  return res.json() as Promise<TResponse>
}

async function fetchWithAuthRetry(
  path: string,
  init: RequestInit,
  retried = false,
): Promise<Response> {
  const headers = new Headers(init.headers)
  const token = useAuthStore.getState().accessToken
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  const res = await fetch(buildApiUrl(path), {
    ...init,
    cache: init.cache ?? 'no-store',
    headers,
    credentials: 'include',
  })
  if (res.status === 401 && !retried && !path.startsWith('/auth/')) {
    const refreshed = await refreshAccessToken()
    if (refreshed) {
      return fetchWithAuthRetry(path, init, true)
    }
    useAuthStore.getState().clearSession()
  }
  return res
}

/** multipart 업로드 (필드명 file). */
export async function postMultipartJson<TResponse>(
  path: string,
  file: File,
): Promise<TResponse> {
  const body = new FormData()
  body.append('file', file)
  const res = await fetchWithAuthRetry(path, { method: 'POST', body })
  await ensureOk(res)
  return res.json() as Promise<TResponse>
}
