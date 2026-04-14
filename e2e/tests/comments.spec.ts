import { expect, test } from '@playwright/test'

import { gateTestsOnHealth, registerUser, uniqueEmail } from './helpers'

gateTestsOnHealth()

test('댓글과 답글을 작성할 수 있다', async ({ page }) => {
  const email = uniqueEmail('e2e-comment')
  await registerUser(page, email)

  await page.getByRole('link', { name: '새 글' }).click()
  await page.getByLabel('제목').fill(`E2E comment post ${Date.now()}`)
  await page.getByLabel('본문').fill('댓글 테스트 본문')
  await page.getByRole('button', { name: '등록' }).click()

  await expect(
    page.getByRole('heading', { name: '댓글', exact: true }),
  ).toBeVisible({ timeout: 20_000 })

  const rootText = `루트 댓글 ${Date.now()}`
  await page.locator('#root-comment').fill(rootText)
  await page.getByRole('button', { name: '댓글 등록' }).click()
  await expect(
    page.locator('pre').filter({ hasText: rootText }).first(),
  ).toBeVisible({ timeout: 15_000 })

  await page.getByRole('button', { name: '답글' }).first().click()
  const replyText = `답글 ${Date.now()}`
  await page.getByLabel('답글 작성').fill(replyText)
  const replySubmit = page.getByRole('button', { name: '답글 등록' }).first()
  await expect(replySubmit).toBeEnabled({ timeout: 10_000 })
  await replySubmit.click()
  await expect(
    page.locator('pre').filter({ hasText: replyText }).first(),
  ).toBeVisible({ timeout: 15_000 })
})
