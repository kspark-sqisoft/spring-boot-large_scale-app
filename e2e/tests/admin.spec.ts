import { expect, test } from '@playwright/test'

import {
  ADMIN_EMAIL,
  ADMIN_PASSWORD,
  gateTestsOnHealth,
  registerUser,
  uniqueEmail,
} from './helpers'

gateTestsOnHealth()

test('부트스트랩 관리자로 사용자 목록에 접근할 수 있다', async ({ page }) => {
  await page.goto('/login')
  await page.locator('#login-email').fill(ADMIN_EMAIL)
  await page.locator('#login-password').fill(ADMIN_PASSWORD)
  await page.getByRole('button', { name: '로그인' }).click()
  await expect(page).toHaveURL(/\/posts/, { timeout: 20_000 })

  await page.getByRole('link', { name: '관리' }).click()
  await expect(page).toHaveURL(/\/admin\/users/)
  await expect(
    page.getByRole('heading', { name: '관리 · 사용자 목록' }),
  ).toBeVisible({ timeout: 15_000 })
  await expect(
    page.getByRole('heading', { name: '사용자', exact: true }),
  ).toBeVisible()
})

test('일반 사용자는 관리 페이지에서 목록으로 돌아간다', async ({ page }) => {
  const email = uniqueEmail('e2e-user')
  await registerUser(page, email)

  await expect(page.getByRole('link', { name: '관리' })).toHaveCount(0)

  await page.goto('/admin/users')
  await expect(page).toHaveURL(/\/posts/, { timeout: 15_000 })
})
