import { Outlet } from 'react-router-dom'

import { AppHeader } from '@/widgets/app-header'

export function AppLayout() {
  return (
    <div className="min-h-dvh">
      <AppHeader />
      <main className="mx-auto max-w-2xl px-4 py-10">
        <Outlet />
      </main>
    </div>
  )
}
