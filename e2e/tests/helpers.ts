import { test } from '@playwright/test'

/** `/api/v1/health` 가 200이 아니면 해당 파일의 테스트를 스킵합니다. 각 spec 상단에서 한 번 호출하세요. */
export function gateTestsOnHealth(): void {
  test.beforeEach(async ({ request }) => {
    const res = await request.get('/api/v1/health')
    test.skip(
      res.status() !== 200,
      '풀스택이 필요합니다. 예: docker compose -f docker-compose.dev.yml up (프론트 5173, API 프록시)',
    )
  })
}

export const DEFAULT_PASSWORD = 'password123'

export function uniqueEmail(prefix: string): string {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@example.com`
}

/** 가입 후 `/posts` 로 이동한 상태를 만듭니다. */
export async function registerUser(
  page: import('@playwright/test').Page,
  email: string,
  password: string = DEFAULT_PASSWORD,
): Promise<void> {
  await page.goto('/register')
  await page.locator('#reg-email').fill(email)
  await page.locator('#reg-password').fill(password)
  await page.getByRole('button', { name: '가입하기' }).click()
  await page.waitForURL(/\/posts/, { timeout: 20_000 })
}

export const ADMIN_EMAIL = process.env.E2E_ADMIN_EMAIL ?? 'admin@board.local'
export const ADMIN_PASSWORD = process.env.E2E_ADMIN_PASSWORD ?? 'ChangeMe_Admin_1'
