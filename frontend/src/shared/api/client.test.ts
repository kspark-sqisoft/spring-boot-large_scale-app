import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { buildApiUrl, HttpError } from './client'
import { useAuthStore } from '@/shared/store/auth-store'

describe('buildApiUrl', () => {
  it('prefixes api v1 path', () => {
    expect(buildApiUrl('/posts')).toBe('/api/v1/posts')
    expect(buildApiUrl('health')).toBe('/api/v1/health')
  })

  it('returns absolute urls unchanged', () => {
    expect(buildApiUrl('https://ex.com/x')).toBe('https://ex.com/x')
  })
})

describe('api client 401 refresh', () => {
  beforeEach(() => {
    useAuthStore.setState({
      accessToken: 'old-access',
      user: {
        id: '1',
        email: 'u@x.com',
        role: 'USER',
        displayName: 'u',
        avatarUrl: null,
      },
      bootstrapped: true,
    })
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
    useAuthStore.getState().clearSession()
  })

  it('retries once after successful refresh', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ accessToken: 'new-access' }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ id: '9', title: 'ok' }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )

    const { getJson } = await import('./client')
    const data = await getJson<{ id: string; title: string }>('/posts/9')

    expect(data.title).toBe('ok')
    expect(useAuthStore.getState().accessToken).toBe('new-access')
    expect(fetchMock).toHaveBeenCalledTimes(3)
    const refreshCall = fetchMock.mock.calls[1]
    expect(String(refreshCall[0])).toContain('/api/v1/auth/refresh')
  })

  it('clears session when refresh fails', async () => {
    const fetchMock = vi.mocked(fetch)
    fetchMock
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(new Response(null, { status: 401 }))

    const { getJson } = await import('./client')
    await expect(getJson('/posts/1')).rejects.toThrow(HttpError)
    expect(useAuthStore.getState().accessToken).toBeNull()
  })
})
