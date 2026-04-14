import { expect, test } from '@playwright/test'

import {
  DEFAULT_PASSWORD,
  gateTestsOnHealth,
  registerUser,
  uniqueEmail,
} from './helpers'

gateTestsOnHealth()

test('로그아웃 후 로그인으로 다시 들어올 수 있다', async ({ page }) => {
  const email = uniqueEmail('e2e-relogin')
  await registerUser(page, email)

  await page.getByRole('button', { name: '로그아웃' }).click()
  await expect(page.getByRole('link', { name: '로그인' })).toBeVisible({
    timeout: 10_000,
  })

  await page.getByRole('link', { name: '로그인' }).click()
  await page.locator('#login-email').fill(email)
  await page.locator('#login-password').fill(DEFAULT_PASSWORD)
  await page.getByRole('button', { name: '로그인' }).click()

  await expect(page).toHaveURL(/\/posts/, { timeout: 20_000 })
  await expect(page.getByRole('link', { name: '새 글' })).toBeVisible()
})

test('프로필에서 표시 이름을 저장할 수 있다', async ({ page }) => {
  const email = uniqueEmail('e2e-profile')
  await registerUser(page, email)

  await page.locator('a[title="프로필"]').click()
  await expect(page.getByRole('heading', { name: '프로필' })).toBeVisible()

  const name = `표시명-${Date.now()}`
  await page.locator('#displayName').fill(name)
  await page.getByRole('button', { name: '저장' }).click()

  await expect(page.locator('a[title="프로필"]')).toContainText(name, {
    timeout: 15_000,
  })
})
