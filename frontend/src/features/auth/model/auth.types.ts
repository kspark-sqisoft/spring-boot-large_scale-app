import { z } from 'zod'

// ── Zod 스키마 (런타임 검증) ─────────────────────────────────────────────
export const AuthUserSchema = z.object({
  id: z.string(),
  email: z.email(),
  role: z.enum(['USER', 'ADMIN']),
  displayName: z.string().nullable().optional(),
  avatarUrl: z.string().nullable().optional(),
})

export const AuthSessionResponseSchema = z.object({
  user: AuthUserSchema,
  accessToken: z.string().min(1),
  expiresInSeconds: z.number(),
  tokenType: z.string(),
})

export const AccessTokenResponseSchema = z.object({
  accessToken: z.string().min(1),
  expiresInSeconds: z.number(),
  tokenType: z.string(),
})

// ── 타입 (스키마에서 추론) ────────────────────────────────────────────────
export type AuthUser = z.infer<typeof AuthUserSchema>
export type AuthSessionResponse = z.infer<typeof AuthSessionResponseSchema>
export type AccessTokenResponse = z.infer<typeof AccessTokenResponseSchema>
export type UserMeResponse = AuthUser
