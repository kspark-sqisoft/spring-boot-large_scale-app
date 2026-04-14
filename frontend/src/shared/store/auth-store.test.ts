import { beforeEach, describe, expect, it } from 'vitest'

import { useAuthStore } from './auth-store'

describe('useAuthStore', () => {
  beforeEach(() => {
    useAuthStore.setState({
      accessToken: null,
      user: null,
      bootstrapped: false,
    })
  })

  it('setSession stores token and user', () => {
    useAuthStore
      .getState()
      .setSession('tok', {
        id: '1',
        email: 'a@b.com',
        role: 'USER',
        displayName: 'a',
        avatarUrl: null,
      })
    const s = useAuthStore.getState()
    expect(s.accessToken).toBe('tok')
    expect(s.user?.email).toBe('a@b.com')
  })

  it('clearSession wipes credentials', () => {
    useAuthStore.getState().setSession('x', {
      id: '1',
      email: 'a@b.com',
      role: 'USER',
      displayName: 'a',
      avatarUrl: null,
    })
    useAuthStore.getState().clearSession()
    const s = useAuthStore.getState()
    expect(s.accessToken).toBeNull()
    expect(s.user).toBeNull()
  })
})
