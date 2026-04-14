import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it } from 'vitest'

import { RequireAuth } from './require-auth'
import { useAuthStore } from '@/shared/store/auth-store'

describe('RequireAuth', () => {
  beforeEach(() => {
    useAuthStore.setState({
      accessToken: null,
      user: null,
      bootstrapped: false,
    })
  })

  it('shows loading copy until bootstrapped', () => {
    render(
      <MemoryRouter initialEntries={['/x']}>
        <Routes>
          <Route
            path="/x"
            element={
              <RequireAuth>
                <span>SECRET</span>
              </RequireAuth>
            }
          />
        </Routes>
      </MemoryRouter>,
    )
    expect(screen.getByText('세션 확인 중…')).toBeInTheDocument()
    expect(screen.queryByText('SECRET')).not.toBeInTheDocument()
  })

  it('renders children when bootstrapped with token', () => {
    useAuthStore.getState().setSession('t', {
      id: '1',
      email: 'a@b.com',
      role: 'USER',
      displayName: 'a',
      avatarUrl: null,
    })
    useAuthStore.setState({ bootstrapped: true })

    render(
      <MemoryRouter initialEntries={['/x']}>
        <Routes>
          <Route
            path="/x"
            element={
              <RequireAuth>
                <span>SECRET</span>
              </RequireAuth>
            }
          />
        </Routes>
      </MemoryRouter>,
    )

    expect(screen.getByText('SECRET')).toBeInTheDocument()
  })

  it('redirects to login when bootstrapped without token', async () => {
    useAuthStore.setState({ bootstrapped: true, accessToken: null, user: null })

    render(
      <MemoryRouter initialEntries={['/secret']}>
        <Routes>
          <Route path="/login" element={<span>LOGIN_SCREEN</span>} />
          <Route
            path="/secret"
            element={
              <RequireAuth>
                <span>SECRET</span>
              </RequireAuth>
            }
          />
        </Routes>
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getByText('LOGIN_SCREEN')).toBeInTheDocument()
    })
  })
})
