import { lazy, Suspense, type ReactElement } from 'react'
import { Box, CircularProgress } from '@mui/material'
import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './state/AuthContext'

const AuthPage = lazy(() => import('./pages/AuthPage'))
const DashboardPage = lazy(() => import('./pages/DashboardPage'))

function ProtectedRoute({ children }: { children: ReactElement }) {
  const { token } = useAuth()
  return token ? children : <Navigate to="/auth" replace />
}

export default function App() {
  const { token } = useAuth()

  return (
    <Suspense
      fallback={
        <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}>
          <CircularProgress />
        </Box>
      }
    >
      <Routes>
        <Route path="/auth" element={token ? <Navigate to="/" replace /> : <AuthPage />} />
        <Route path="/" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
        <Route path="*" element={<Navigate to={token ? '/' : '/auth'} replace />} />
      </Routes>
    </Suspense>
  )
}
