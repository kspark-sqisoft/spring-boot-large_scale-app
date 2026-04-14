import { expect, test } from '@playwright/test'

import { gateTestsOnHealth } from './helpers'

gateTestsOnHealth()

test('게시글 목록 화면이 로드된다', async ({ page }) => {
  await page.goto('/posts')
  await expect(page.getByRole('heading', { name: '게시글' })).toBeVisible()
})

test('회원가입 후 새 글을 작성할 수 있다', async ({ page }) => {
  const email = `e2e-${Date.now()}@example.com`

  await page.goto('/register')
  await page.locator('#reg-email').fill(email)
  await page.locator('#reg-password').fill('password123')
  await page.getByRole('button', { name: '가입하기' }).click()

  await expect(page).toHaveURL(/\/posts/, { timeout: 20_000 })

  await page.getByRole('link', { name: '새 글' }).click()
  await expect(page.getByRole('heading', { name: '새 게시글' })).toBeVisible()

  await page.getByLabel('제목').fill(`E2E 제목 ${Date.now()}`)
  await page.getByLabel('본문').fill('Playwright로 작성한 본문입니다.')
  await page.getByRole('button', { name: '등록' }).click()

  await expect(
    page.locator('pre').filter({ hasText: 'Playwright로 작성한 본문입니다.' }),
  ).toBeVisible({ timeout: 20_000 })
})

test('비로그인 사용자는 작성 페이지에서 로그인으로 보내진다', async ({ page }) => {
  await page.goto('/posts/new')
  await expect(page).toHaveURL(/\/login/, { timeout: 10_000 })
})
