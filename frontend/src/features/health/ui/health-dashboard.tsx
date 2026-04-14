import { useActionState } from 'react'
import { useFormStatus } from 'react-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Loader2, RefreshCw } from 'lucide-react'
import { toast } from 'sonner'

import { fetchHealth } from '@/features/health/api/health-api'
import { healthKeys } from '@/features/health/api/health-keys'
import { Badge } from '@/shared/ui/badge'
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
import { getJson } from '@/shared/api/client'
import { useUiStore } from '@/shared/stores/ui-store'

type RefreshFormState =
  | null
  | { ok: true; message: string; at: number }
  | { ok: false; message: string }

function RefetchSubmitButton() {
  const { pending } = useFormStatus()

  return (
    <Button
      type="submit"
      disabled={pending}
      variant="secondary"
      className="w-full sm:w-auto"
    >
      {pending ? (
        <>
          <Loader2 className="size-4 animate-spin" aria-hidden />
          확인 중…
        </>
      ) : (
        <>
          <RefreshCw className="size-4" aria-hidden />
          서버 상태 다시 확인
        </>
      )}
    </Button>
  )
}

export function HealthDashboard() {
  const queryClient = useQueryClient()
  const boardTitle = useUiStore((s) => s.boardTitle)
  const setBoardTitle = useUiStore((s) => s.setBoardTitle)
  const setLastFormMessage = useUiStore((s) => s.setLastFormMessage)

  const healthQuery = useQuery({
    queryKey: healthKeys.status(),
    queryFn: fetchHealth,
  })

  const [formState, formAction] = useActionState(
    async (
      _previous: RefreshFormState | null,
      _formData: FormData,
    ): Promise<RefreshFormState | null> => {
      void _previous
      void _formData
      try {
        await getJson('/health')
        await queryClient.invalidateQueries({ queryKey: healthKeys.all })
        toast.success('백엔드와 다시 통신했어요')
        const message = '최신 상태를 반영했습니다'
        setLastFormMessage(message)
        return { ok: true, message, at: Date.now() }
      } catch (e) {
        const message = e instanceof Error ? e.message : '알 수 없는 오류'
        toast.error(message)
        setLastFormMessage(null)
        return { ok: false, message }
      }
    },
    null,
  )

  const data = healthQuery.data
  const showError = healthQuery.isError
  const errorMessage =
    healthQuery.error instanceof Error
      ? healthQuery.error.message
      : '불러오기 실패'

  return (
    <div className="space-y-8">
      <div className="space-y-1">
        <h1 className="text-3xl font-bold tracking-tight">{boardTitle}</h1>
        <p className="text-muted-foreground">
          FSD · React 19 · TanStack Query · Zustand · shadcn/ui · Tailwind
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>보드 표시 이름</CardTitle>
          <CardDescription>
            Zustand(`shared/stores`)로 전역 UI 상태를 분리합니다.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-2">
          <Label htmlFor="board-title">제목</Label>
          <Input
            id="board-title"
            value={boardTitle}
            onChange={(e) => setBoardTitle(e.target.value)}
            placeholder="기술 게시판"
            autoComplete="off"
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex flex-row items-start justify-between gap-4 space-y-0">
          <div className="space-y-1.5">
            <CardTitle>백엔드 상태</CardTitle>
            <CardDescription>
              <code className="rounded bg-muted px-1 py-0.5 text-xs">
                features/health
              </code>{' '}
              + Query · 폼{' '}
              <code className="rounded bg-muted px-1 py-0.5 text-xs">
                action
              </code>{' '}
              /
              <code className="rounded bg-muted px-1 py-0.5 text-xs">
                useFormStatus
              </code>
            </CardDescription>
          </div>
          {healthQuery.isFetching ? (
            <Badge variant="secondary" className="shrink-0">
              동기화 중
            </Badge>
          ) : null}
        </CardHeader>
        <CardContent className="space-y-4">
          {showError ? (
            <p className="text-sm text-destructive" role="alert">
              {errorMessage}
            </p>
          ) : null}

          {!showError && healthQuery.isPending ? (
            <p className="text-sm text-muted-foreground">불러오는 중…</p>
          ) : null}

          {data ? (
            <div className="flex flex-wrap items-center gap-3">
              <Badge
                variant={data.status === 'UP' ? 'success' : 'secondary'}
                className="uppercase"
              >
                {data.status}
              </Badge>
              <span className="text-sm text-muted-foreground">
                서비스: <span className="font-medium">{data.service}</span>
              </span>
            </div>
          ) : null}

          {data ? (
            <pre className="max-h-48 overflow-auto rounded-md border bg-muted/50 p-3 text-xs leading-relaxed">
              {JSON.stringify(data, null, 2)}
            </pre>
          ) : null}

          <form action={formAction} className="flex flex-col gap-3 sm:flex-row sm:items-center">
            <RefetchSubmitButton />
            {formState?.ok ? (
              <p
                className="animate-in fade-in slide-in-from-bottom-1 text-sm font-medium text-emerald-600 duration-200 dark:text-emerald-400"
                role="status"
              >
                {formState.message}
              </p>
            ) : null}
            {formState && !formState.ok ? (
              <p
                className="animate-in fade-in text-sm text-destructive duration-200"
                role="alert"
              >
                {formState.message}
              </p>
            ) : null}
          </form>
        </CardContent>
        <CardFooter className="text-xs text-muted-foreground">
          Sonner 토스트와 인라인 메시지로 긍정적 피드백을 동시에 보여 줍니다.
        </CardFooter>
      </Card>
    </div>
  )
}
