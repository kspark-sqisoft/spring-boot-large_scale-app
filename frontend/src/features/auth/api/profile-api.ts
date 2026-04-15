import { patchJson } from '@/shared/api/client'

import type { UserMeResponse } from '../model/auth.types'

// PATCH /users/me — Bearer 필요

export type UpdateProfileBody = {
  displayName?: string
  avatarFileId?: string
}

export function updateProfile(body: UpdateProfileBody): Promise<UserMeResponse> {
  return patchJson<UserMeResponse>('/users/me', body)
}
