import { QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import { ThemeProvider } from 'next-themes'
import { useState, type ReactNode } from 'react'

import { createAppQueryClient } from '@/app/query-client'
import { AuthBootstrap } from '@/features/auth'
import { Toaster } from '@/shared/ui/sonner'

export function AppProviders({ children }: { children: ReactNode }) {
  const [queryClient] = useState(createAppQueryClient)

  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider attribute="class" defaultTheme="system" enableSystem>
        <AuthBootstrap>{children}</AuthBootstrap>
        <Toaster richColors closeButton position="top-center" />
      </ThemeProvider>
      {import.meta.env.DEV ? <ReactQueryDevtools initialIsOpen={false} /> : null}
    </QueryClientProvider>
  )
}
