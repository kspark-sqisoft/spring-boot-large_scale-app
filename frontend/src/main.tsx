import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'

import App from '@/app/App'
import { AppProviders } from '@/app/providers'
import '@/index.css'

// React 18 루트: 전역 Provider(React Query·테마·세션 복구) 아래에 라우터 App
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AppProviders>
      <App />
    </AppProviders>
  </StrictMode>,
)
