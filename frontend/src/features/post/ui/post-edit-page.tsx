import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { toast } from 'sonner'

import { fetchPost, updatePost } from '@/features/post/api/post-api'
import { postKeys } from '@/features/post/api/post-keys'
import type { PostDto } from '@/features/post/model/post.types'
import { uploadImage } from '@/features/file/api/upload-api'
import { Button } from '@/shared/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/shared/ui/card'
import { Input } from '@/shared/ui/input'
import { Label } from '@/shared/ui/label'

// 기존 글 fetch 후 폼에서 updatePost(이미지 id 목록 교체 가능)

function PostEditForm({ postId, initial }: { postId: string; initial: PostDto }) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [title, setTitle] = useState(initial.title)
  const [content, setContent] = useState(initial.content)
  const [imageIds, setImageIds] = useState<string[]>(() =>
    initial.images.map((i) => i.id),
  )
  const [newFiles, setNewFiles] = useState<File[]>([])

  const mutation = useMutation({
    mutationFn: async () => {
      const uploaded: string[] = []
      for (const file of newFiles) {
        const up = await uploadImage(file)
        uploaded.push(up.id)
      }
      return updatePost(postId, {
        title: title.trim(),
        content: content.trim(),
        imageFileIds: [...imageIds, ...uploaded],
      })
    },
    onSuccess: (post) => {
      void queryClient.invalidateQueries({ queryKey: postKeys.lists() })
      void queryClient.setQueryData(postKeys.detail(post.id), post)
      setNewFiles([])
      setImageIds(post.images.map((i) => i.id))
      toast.success('저장했습니다.')
      void navigate(`/posts/${post.id}`)
    },
    onError: (e: Error) => toast.error(e.message),
  })

  return (
    <Card>
      <CardHeader>
        <CardTitle>내용</CardTitle>
        <CardDescription>수정 후 저장하세요.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-2">
          <Label htmlFor="edit-title">제목</Label>
          <Input
            id="edit-title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            maxLength={500}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="edit-content">본문</Label>
          <textarea
            id="edit-content"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            maxLength={10_000}
            rows={10}
            className="flex min-h-[160px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
          />
        </div>
        <div className="space-y-2">
          <Label>첨부 이미지</Label>
          {imageIds.length > 0 ? (
            <ul className="flex flex-wrap gap-2">
              {initial.images
                .filter((img) => imageIds.includes(img.id))
                .map((img) => (
                  <li
                    key={img.id}
                    className="relative h-20 w-20 overflow-hidden rounded border"
                  >
                    <img
                      src={img.url}
                      alt=""
                      className="h-full w-full object-cover"
                    />
                    <button
                      type="button"
                      className="absolute right-0 top-0 bg-destructive/90 px-1 text-[10px] text-destructive-foreground"
                      onClick={() =>
                        setImageIds((ids) => ids.filter((x) => x !== img.id))
                      }
                    >
                      ×
                    </button>
                  </li>
                ))}
            </ul>
          ) : initial.images.length > 0 ? (
            <p className="text-xs text-muted-foreground">
              저장하면 기존 이미지가 모두 제거됩니다.
            </p>
          ) : (
            <p className="text-xs text-muted-foreground">등록된 이미지 없음</p>
          )}
          <Input
            type="file"
            accept="image/jpeg,image/png,image/gif,image/webp"
            multiple
            onChange={(e) => {
              const list = e.target.files ? Array.from(e.target.files) : []
              setNewFiles((prev) => {
                const cap = 10 - imageIds.length - prev.length
                const add = list.slice(0, Math.max(0, cap))
                return [...prev, ...add]
              })
              e.target.value = ''
            }}
          />
          {newFiles.length > 0 ? (
            <ul className="text-xs text-muted-foreground">
              {newFiles.map((f, i) => (
                <li key={`${f.name}-${i}`} className="flex items-center gap-2">
                  <span className="max-w-[200px] truncate">{f.name}</span>
                  <button
                    type="button"
                    className="text-destructive underline"
                    onClick={() =>
                      setNewFiles((prev) => prev.filter((_, j) => j !== i))
                    }
                  >
                    삭제
                  </button>
                </li>
              ))}
            </ul>
          ) : null}
          <p className="text-xs text-muted-foreground">
            저장 시 게시글에 연결되는 이미지가 위 목록과 새 파일로 교체됩니다. (최대 10장)
          </p>
        </div>
      </CardContent>
      <CardFooter className="gap-2">
        <Button
          type="button"
          disabled={
            mutation.isPending ||
            !title.trim() ||
            !content.trim() ||
            imageIds.length + newFiles.length > 10
          }
          onClick={() => mutation.mutate()}
        >
          {mutation.isPending ? '저장 중…' : '저장'}
        </Button>
        <Button type="button" variant="outline" asChild>
          <Link to={`/posts/${postId}`}>취소</Link>
        </Button>
      </CardFooter>
    </Card>
  )
}

export function PostEditPage() {
  const { postId } = useParams<{ postId: string }>()
  const { data, isPending, isError } = useQuery({
    queryKey: postKeys.detail(postId ?? ''),
    queryFn: () => fetchPost(postId!),
    enabled: Boolean(postId),
  })

  if (!postId) {
    return <p className="text-destructive">잘못된 경로입니다.</p>
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="sm" asChild>
          <Link to={`/posts/${postId}`}>← 상세</Link>
        </Button>
        <h1 className="text-2xl font-bold tracking-tight">게시글 수정</h1>
      </div>

      {isPending && !data ? (
        <p className="text-sm text-muted-foreground">불러오는 중…</p>
      ) : null}
      {isError ? (
        <p className="text-sm text-destructive">불러오기 실패</p>
      ) : null}

      {data ? (
        <PostEditForm
          key={`${data.id}-${data.updatedAt}`}
          postId={postId}
          initial={data}
        />
      ) : null}
    </div>
  )
}
