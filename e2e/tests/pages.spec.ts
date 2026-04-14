import { expect, test } from '@playwright/test'

import { gateTestsOnHealth } from './helpers'

gateTestsOnHealth()

test('상태(헬스) 대시보드에 백엔드 UP이 보인다', async ({ page }) => {
  await page.goto('/health')
  await expect(page.getByRole('heading', { name: '기술 게시판' })).toBeVisible()
  await expect(page.getByRole('heading', { name: '백엔드 상태' })).toBeVisible()
  await expect(page.getByText('UP', { exact: true })).toBeVisible({ timeout: 20_000 })
})

test('인기 글 페이지가 로드된다', async ({ page }) => {
  await page.goto('/posts/popular')
  await expect(page.getByRole('heading', { name: '인기 글' })).toBeVisible()
  await expect(page.getByText(/조회 이벤트가 Kafka/)).toBeVisible()
})

test('헤더에서 인기로 이동할 수 있다', async ({ page }) => {
  await page.goto('/posts')
  await page.getByRole('link', { name: '인기' }).click()
  await expect(page).toHaveURL(/\/posts\/popular/)
  await expect(page.getByRole('heading', { name: '인기 글' })).toBeVisible()
})
