import { createBrowserRouter, Navigate } from 'react-router-dom'

import {
  AdminOnly,
  AdminUsersPage,
  LoginPage,
  ProfilePage,
  RegisterPage,
  RequireAuth,
} from '@/features/auth'
import { HealthDashboard } from '@/features/health'
import {
  PostCreatePage,
  PostDetailPage,
  PostEditPage,
  PostListPage,
} from '@/features/post'
import { AppLayout } from '@/widgets/app-layout'

export const appRouter = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <Navigate to="/posts" replace /> },
      { path: 'login', element: <LoginPage /> },
      { path: 'register', element: <RegisterPage /> },
      { path: 'posts', element: <PostListPage /> },
      {
        path: 'posts/new',
        element: (
          <RequireAuth>
            <PostCreatePage />
          </RequireAuth>
        ),
      },
      {
        path: 'posts/:postId/edit',
        element: (
          <RequireAuth>
            <PostEditPage />
          </RequireAuth>
        ),
      },
      { path: 'posts/:postId', element: <PostDetailPage /> },
      {
        path: 'profile',
        element: (
          <RequireAuth>
            <ProfilePage />
          </RequireAuth>
        ),
      },
      { path: 'health', element: <HealthDashboard /> },
      {
        path: 'admin/users',
        element: (
          <RequireAuth>
            <AdminOnly>
              <AdminUsersPage />
            </AdminOnly>
          </RequireAuth>
        ),
      },
    ],
  },
])
