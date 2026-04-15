import { create } from 'zustand'

// 메모리의 액세스 토큰 + 프로필(리프레시는 HttpOnly 쿠키라 JS에서 안 보임)
export type AuthUser = {
  id: string
  email: string
  role: string
  displayName: string
  avatarUrl: string | null
}

type AuthState = {
  accessToken: string | null
  user: AuthUser | null
  /** AuthBootstrap의 refresh 시도가 끝났는지 — RequireAuth가 이걸 보고 리다이렉트 */
  bootstrapped: boolean
  setSession: (accessToken: string, user: AuthUser) => void
  setAccessToken: (accessToken: string) => void
  setUser: (user: AuthUser) => void
  clearSession: () => void
  setBootstrapped: (v: boolean) => void
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  user: null,
  bootstrapped: false,
  setSession: (accessToken, user) => set({ accessToken, user }),
  setAccessToken: (accessToken) => set({ accessToken }),
  setUser: (user) => set({ user }),
  clearSession: () => set({ accessToken: null, user: null }),
  setBootstrapped: (bootstrapped) => set({ bootstrapped }),
}))
