import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { useAuthStore } from '@/shared/store/auth-store'

import { loginRequest, logoutRequest, registerRequest } from './auth-api'

const mockSession = {
  user: {
    id: '1',
    email: 'user@test.com',
    role: 'USER' as const,
    displayName: '테스터',
    avatarUrl: null,
  },
  accessToken: 'tok_abc',
  expiresInSeconds: 900,
  tokenType: 'Bearer',
}

function mockFetch(body: unknown, status = 200) {
  return vi.fn().mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
  })
}

beforeEach(() => {
  useAuthStore.setState({ accessToken: null, user: null, bootstrapped: false })
})

afterEach(() => {
  vi.restoreAllMocks()
})

describe('loginRequest', () => {
  it('성공 시 세션을 store에 저장하고 응답 반환', async () => {
    vi.stubGlobal('fetch', mockFetch(mockSession))

    const result = await loginRequest('user@test.com', 'password123')

    expect(result.accessToken).toBe('tok_abc')
    expect(useAuthStore.getState().accessToken).toBe('tok_abc')
    expect(useAuthStore.getState().user?.email).toBe('user@test.com')
  })

  it('서버 오류 시 HttpError를 throw하고 store는 비어 있음', async () => {
    vi.stubGlobal(
      'fetch',
      mockFetch({ message: '이메일 또는 비밀번호가 올바르지 않습니다.', code: 'AUTH_FAILED' }, 401),
    )

    await expect(loginRequest('wrong@test.com', 'bad')).rejects.toThrow()
    expect(useAuthStore.getState().accessToken).toBeNull()
  })

  it('응답 스키마가 맞지 않으면 ZodError throw', async () => {
    vi.stubGlobal('fetch', mockFetch({ invalid: 'shape' }))

    await expect(loginRequest('user@test.com', 'password123')).rejects.toThrow()
  })

  it('알 수 없는 role 값이면 ZodError throw', async () => {
    vi.stubGlobal(
      'fetch',
      mockFetch({ ...mockSession, user: { ...mockSession.user, role: 'SUPERUSER' } }),
    )

    await expect(loginRequest('user@test.com', 'password123')).rejects.toThrow()
  })
})

describe('registerRequest', () => {
  it('성공 시 세션 저장', async () => {
    vi.stubGlobal('fetch', mockFetch(mockSession, 201))

    const result = await registerRequest('new@test.com', 'password123')

    expect(result.user.email).toBe('user@test.com')
    expect(useAuthStore.getState().user).not.toBeNull()
  })

  it('이메일 중복 시 오류 throw', async () => {
    vi.stubGlobal(
      'fetch',
      mockFetch({ message: '이미 사용 중인 이메일입니다.', code: 'EMAIL_TAKEN' }, 409),
    )

    await expect(registerRequest('dup@test.com', 'password123')).rejects.toThrow()
  })
})

describe('logoutRequest', () => {
  it('성공 시 store 세션을 비운다', async () => {
    useAuthStore.setState({ accessToken: 'tok_abc', user: mockSession.user })
    vi.stubGlobal('fetch', mockFetch(null, 200))

    await logoutRequest()

    expect(useAuthStore.getState().accessToken).toBeNull()
    expect(useAuthStore.getState().user).toBeNull()
  })

  it('네트워크 오류가 나도 store 세션을 비운다', async () => {
    useAuthStore.setState({ accessToken: 'tok_abc', user: mockSession.user })
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('network')))

    await expect(logoutRequest()).rejects.toThrow()
    expect(useAuthStore.getState().accessToken).toBeNull()
  })
})
