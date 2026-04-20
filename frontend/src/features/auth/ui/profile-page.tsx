import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { toast } from 'sonner'

import { fetchCurrentUser } from '@/features/auth/api/auth-api'
import { updateProfile } from '@/features/auth/api/profile-api'
import { uploadImage } from '@/features/file/api/upload-api'
import { useAuthStore } from '@/shared/store/auth-store'
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

function useBlobUrlPreview(file: File | null): string | null {
  const [url, setUrl] = useState<string | null>(null)

  useEffect(() => {
    if (!file) {
      const id = requestAnimationFrame(() => {
        setUrl(null)
      })
      return () => cancelAnimationFrame(id)
    }

    const objectUrl = URL.createObjectURL(file)
    const id = requestAnimationFrame(() => {
      setUrl(objectUrl)
    })
    return () => {
      cancelAnimationFrame(id)
      URL.revokeObjectURL(objectUrl)
    }
  }, [file])

  return url
}

// `/users/me` GET/PATCH + 아바타 업로드 후 zustand user 동기화

const meQueryKey = ['users', 'me'] as const

export function ProfilePage() {
  const queryClient = useQueryClient()
  const setUser = useAuthStore((s) => s.setUser)
  const [displayName, setDisplayName] = useState('')
  const [pendingAvatarFile, setPendingAvatarFile] = useState<File | null>(null)
  const pendingAvatarPreview = useBlobUrlPreview(pendingAvatarFile)

  const { data: me, isPending, isError } = useQuery({
    queryKey: meQueryKey,
    queryFn: fetchCurrentUser,
  })

  const [appliedMeSyncToken, setAppliedMeSyncToken] = useState('')
  if (me) {
    const nextMeSyncToken = `${me.id}\u0000${me.displayName}`
    if (nextMeSyncToken !== appliedMeSyncToken) {
      setAppliedMeSyncToken(nextMeSyncToken)
      setDisplayName(me.displayName)
    }
  }

  const saveMutation = useMutation({
    mutationFn: async () => {
      let avatarFileId: string | undefined
      if (pendingAvatarFile) {
        const up = await uploadImage(pendingAvatarFile)
        avatarFileId = up.id
      }
      const body: { displayName: string; avatarFileId?: string } = {
        displayName: displayName.trim(),
      }
      if (avatarFileId !== undefined) {
        body.avatarFileId = avatarFileId
      }
      return updateProfile(body)
    },
    onSuccess: (user) => {
      setUser(user)
      void queryClient.invalidateQueries({ queryKey: meQueryKey })
      setPendingAvatarFile(null)
      toast.success('프로필을 저장했습니다.')
    },
    onError: (e: Error) => toast.error(e.message),
  })

  const clearAvatarMutation = useMutation({
    mutationFn: () => updateProfile({ avatarFileId: '' }),
    onSuccess: (user) => {
      setUser(user)
      void queryClient.invalidateQueries({ queryKey: meQueryKey })
      setPendingAvatarFile(null)
      toast.success('프로필 이미지를 제거했습니다.')
    },
    onError: (e: Error) => toast.error(e.message),
  })

  const previewSrc = pendingAvatarPreview ?? me?.avatarUrl ?? null

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="sm" asChild>
          <Link to="/posts">← 게시글</Link>
        </Button>
        <h1 className="text-2xl font-bold tracking-tight">프로필</h1>
      </div>

      {isPending && !me ? (
        <p className="text-sm text-muted-foreground">불러오는 중…</p>
      ) : null}
      {isError ? (
        <p className="text-sm text-destructive">프로필을 불러오지 못했습니다.</p>
      ) : null}

      {me ? (
        <Card>
          <CardHeader>
            <CardTitle>내 정보</CardTitle>
            <CardDescription>
              표시 이름과 프로필 이미지를 바꿀 수 있습니다. 이메일: {me.email}
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-start">
              <div className="shrink-0">
                {previewSrc ? (
                  <img
                    src={previewSrc}
                    alt=""
                    className="h-24 w-24 rounded-full border object-cover"
                  />
                ) : (
                  <div className="flex h-24 w-24 items-center justify-center rounded-full border border-dashed text-xs text-muted-foreground">
                    이미지 없음
                  </div>
                )}
              </div>
              <div className="flex flex-1 flex-col gap-3">
                <div className="space-y-2">
                  <Label htmlFor="avatar">프로필 이미지</Label>
                  <Input
                    id="avatar"
                    type="file"
                    accept="image/jpeg,image/png,image/gif,image/webp"
                    onChange={(e) => {
                      const f = e.target.files?.[0]
                      setPendingAvatarFile(f ?? null)
                    }}
                  />
                  <p className="text-xs text-muted-foreground">
                    JPEG, PNG, GIF, WebP · 최대 5MB
                  </p>
                </div>
                {me.avatarUrl && !pendingAvatarFile ? (
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="w-fit"
                    disabled={clearAvatarMutation.isPending}
                    onClick={() => clearAvatarMutation.mutate()}
                  >
                    {clearAvatarMutation.isPending ? '제거 중…' : '이미지 제거'}
                  </Button>
                ) : null}
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="displayName">표시 이름</Label>
              <Input
                id="displayName"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                maxLength={100}
                placeholder="이름"
              />
            </div>
          </CardContent>
          <CardFooter className="gap-2">
            <Button
              type="button"
              disabled={saveMutation.isPending}
              onClick={() => saveMutation.mutate()}
            >
              {saveMutation.isPending ? '저장 중…' : '저장'}
            </Button>
          </CardFooter>
        </Card>
      ) : null}
    </div>
  )
}
