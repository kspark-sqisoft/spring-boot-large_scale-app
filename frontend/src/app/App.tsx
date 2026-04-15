import { RouterProvider } from 'react-router-dom'

import { appRouter } from '@/app/router'

// 라우트 정의는 router.tsx — 여기서는 브라우저 라우터만 연결
export default function App() {
  return <RouterProvider router={appRouter} />
}
