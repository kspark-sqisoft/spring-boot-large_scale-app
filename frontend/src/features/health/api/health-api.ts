import { getJson } from '@/shared/api/client'

import type { HealthDto } from '../model/health.types'

/** 공개 엔드포인트 — 인증 없이 상태 JSON */
export async function fetchHealth(): Promise<HealthDto> {
  return getJson<HealthDto>('/health')
}
