import { expect, test } from '@playwright/test'

import { gateTestsOnHealth, registerUser, uniqueEmail } from './helpers'

gateTestsOnHealth()

test('게시글 상세에서 좋아요를 누를 수 있다', async ({ page }) => {
  const email = uniqueEmail('e2e-like')
  await registerUser(page, email)

  await page.getByRole('link', { name: '새 글' }).click()
  const title = `좋아요용 ${Date.now()}`
  await page.getByLabel('제목').fill(title)
  await page.getByLabel('본문').fill('본문')
  await page.getByRole('button', { name: '등록' }).click()
  await expect(page.getByRole('heading', { name: title })).toBeVisible({
    timeout: 20_000,
  })

  const likeBtn = page.getByRole('button', { name: /♡ 좋아요/ })
  await expect(likeBtn).toBeVisible()
  await likeBtn.click()
  await expect(page.getByRole('button', { name: /♥ 좋아요 취소/ })).toBeVisible({
    timeout: 15_000,
  })
})

test('본인 글을 수정·삭제할 수 있다', async ({ page }) => {
  const email = uniqueEmail('e2e-crud')
  await registerUser(page, email)

  await page.getByRole('link', { name: '새 글' }).click()
  const originalTitle = `수정전 ${Date.now()}`
  await page.getByLabel('제목').fill(originalTitle)
  await page.getByLabel('본문').fill('원본 본문')
  await page.getByRole('button', { name: '등록' }).click()
  await expect(page.getByRole('heading', { name: originalTitle })).toBeVisible({
    timeout: 20_000,
  })

  await page.getByRole('link', { name: '수정' }).click()
  await expect(page.getByRole('heading', { name: '게시글 수정' })).toBeVisible()

  const edited = `수정후 ${Date.now()}`
  await page.locator('#edit-title').fill(edited)
  await page.getByRole('button', { name: '저장' }).click()
  await expect(page.getByRole('heading', { name: edited })).toBeVisible({
    timeout: 20_000,
  })

  page.once('dialog', (d) => d.accept())
  await page.getByRole('button', { name: '삭제' }).click()
  await expect(page).toHaveURL(/\/posts$/, { timeout: 20_000 })
  await expect(page.getByRole('heading', { name: '게시글' })).toBeVisible()
})
