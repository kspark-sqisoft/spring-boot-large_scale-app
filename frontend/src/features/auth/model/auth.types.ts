import type { AuthUser } from '@/shared/store/auth-store'

export type AuthSessionResponse = {
  user: AuthUser
  accessToken: string
  expiresInSeconds: number
  tokenType: string
}

export type UserMeResponse = AuthUser

export type AccessTokenResponse = {
  accessToken: string
  expiresInSeconds: number
  tokenType: string
}
