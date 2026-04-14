export const commentKeys = {
  all: ['comments'] as const,
  post: (postId: string) => [...commentKeys.all, postId] as const,
}
