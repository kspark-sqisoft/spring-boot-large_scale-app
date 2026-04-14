export const postKeys = {
  all: ['posts'] as const,
  lists: () => [...postKeys.all, 'list'] as const,
  /** 목록 무한 스크롤 (커서 API) */
  infiniteList: (size: number) => [...postKeys.lists(), 'infinite', size] as const,
  details: () => [...postKeys.all, 'detail'] as const,
  detail: (id: string) => [...postKeys.details(), id] as const,
  popular: (limit: number) => [...postKeys.all, 'popular', limit] as const,
}
