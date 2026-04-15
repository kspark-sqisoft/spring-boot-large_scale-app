import { QueryClient } from '@tanstack/react-query'

// useInfiniteQuery 등 전역 기본: staleTime·재시도 횟수
export function createAppQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        retry: 1,
      },
    },
  })
}
