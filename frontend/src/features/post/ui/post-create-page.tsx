import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'

import { createPost } from '@/features/post/api/post-api'
import { postKeys } from '@/features/post/api/post-keys'
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

export function PostCreatePage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [pendingFiles, setPendingFiles] = useState<File[]>([])

  const mutation = useMutation({
    mutationFn: async () => {
      const imageFileIds: string[] = []
      for (const file of pendingFiles) {
        const up = await uploadImage(file)
        imageFileIds.push(up.id)
      }
      return createPost({
        title: title.trim(),
        content: content.trim(),
        imageFileIds: imageFileIds.length > 0 ? imageFileIds : undefined,
      })
    },
    onSuccess: (post) => {
      void queryClient.invalidateQueries({ queryKey: postKeys.lists() })
      toast.success('게시글이 등록되었습니다.')
      void navigate(`/posts/${post.id}`)
    },
    onError: (e: Error) => {
      toast.error(e.message)
    },
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="sm" asChild>
          <Link to="/posts">← 목록</Link>
        </Button>
        <h1 className="text-2xl font-bold tracking-tight">새 게시글</h1>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>내용</CardTitle>
          <CardDescription>제목과 본문을 입력하세요.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="title">제목</Label>
            <Input
              id="title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              maxLength={500}
              placeholder="제목"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="images">이미지 (선택, 최대 10장)</Label>
            <Input
              id="images"
              type="file"
              accept="image/jpeg,image/png,image/gif,image/webp"
              multiple
              onChange={(e) => {
                const list = e.target.files ? Array.from(e.target.files) : []
                setPendingFiles((prev) => {
                  const next = [...prev, ...list].slice(0, 10)
                  return next
                })
                e.target.value = ''
              }}
            />
            {pendingFiles.length > 0 ? (
              <ul className="flex flex-wrap gap-2 text-xs text-muted-foreground">
                {pendingFiles.map((f, i) => (
                  <li
                    key={`${f.name}-${i}`}
                    className="flex items-center gap-1 rounded border px-2 py-1"
                  >
                    <span className="max-w-[140px] truncate">{f.name}</span>
                    <button
                      type="button"
                      className="text-destructive underline"
                      onClick={() =>
                        setPendingFiles((prev) => prev.filter((_, j) => j !== i))
                      }
                    >
                      삭제
                    </button>
                  </li>
                ))}
              </ul>
            ) : null}
          </div>
          <div className="space-y-2">
            <Label htmlFor="content">본문</Label>
            <textarea
              id="content"
              value={content}
              onChange={(e) => setContent(e.target.value)}
              maxLength={10_000}
              rows={10}
              placeholder="본문 (최대 10,000자)"
              className="flex min-h-[160px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
            />
          </div>
        </CardContent>
        <CardFooter className="gap-2">
          <Button
            type="button"
            disabled={mutation.isPending || !title.trim() || !content.trim()}
            onClick={() => mutation.mutate()}
          >
            {mutation.isPending ? '등록 중…' : '등록'}
          </Button>
          <Button type="button" variant="outline" asChild>
            <Link to="/posts">취소</Link>
          </Button>
        </CardFooter>
      </Card>
    </div>
  )
}
