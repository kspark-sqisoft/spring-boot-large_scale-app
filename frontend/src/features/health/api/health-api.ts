import { getJson } from '@/shared/api/client'

import type { HealthDto } from '../model/health.types'

export async function fetchHealth(): Promise<HealthDto> {
  return getJson<HealthDto>('/health')
}
