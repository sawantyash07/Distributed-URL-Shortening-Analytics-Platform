import { useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Container,
  Paper,
  Stack,
  Tab,
  Tabs,
  TextField,
  Typography,
} from '@mui/material'
import BoltIcon from '@mui/icons-material/Bolt'
import SecurityIcon from '@mui/icons-material/Security'
import InsightsIcon from '@mui/icons-material/Insights'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import { useAuth } from '../state/AuthContext'
import type { AuthResponse } from '../types'

type Mode = 'login' | 'register'

export default function AuthPage() {
  const [mode, setMode] = useState<Mode>('login')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [form, setForm] = useState({
    fullName: '',
    email: '',
    password: '',
  })
  const { login } = useAuth()
  const navigate = useNavigate()

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    setLoading(true)
    setError(null)
    setSuccess(null)

    try {
      if (mode === 'login') {
        const { data } = await api.post<AuthResponse>('/api/auth/login', {
          email: form.email,
          password: form.password,
        })
        login(data)
        navigate('/', { replace: true })
      } else {
        await api.post<AuthResponse>('/api/auth/register', {
          fullName: form.fullName,
          email: form.email,
          password: form.password,
        })
        setMode('login')
        setForm((current) => ({ ...current, fullName: '', password: '' }))
        setSuccess('Registration successful. Please log in to access the dashboard.')
      }
    } catch (err: any) {
      const status = err?.response?.status
      const message = err?.response?.data?.message

      if (mode === 'register' && status === 409) {
        setMode('login')
        setError(null)
        setSuccess('An account with this email already exists. Please log in with your existing password.')
      } else if (mode === 'login' && status === 401) {
        setError('Invalid email or password. If this is a new account, register first, then log in with the same password.')
      } else {
        setError(message ?? 'Unable to authenticate. Please try again.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <Box sx={{ minHeight: '100vh', py: 6, display: 'flex', alignItems: 'center' }}>
      <Container maxWidth="lg">
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={4} alignItems="stretch">
          <Paper sx={{ flex: 1, p: { xs: 3, md: 5 }, background: 'linear-gradient(160deg, rgba(15,118,110,0.93), rgba(21,94,89,0.88))', color: 'white' }}>
            <Stack spacing={3}>
              <Typography variant="overline" sx={{ letterSpacing: 2, opacity: 0.8 }}>
                DISTRIBUTED URL SHORTENER
              </Typography>
              <Typography variant="h2">
                Operate millions of links with a control plane built for interviews and production conversations.
              </Typography>
              <Typography variant="h6" sx={{ opacity: 0.85 }}>
                JWT auth, Redis cache-aside reads, PostgreSQL analytics, Docker, Kubernetes, and Render deployment baked into one project.
              </Typography>
              <Stack spacing={2} pt={2}>
                {[
                  { label: 'Base62 slugs and cache hits tuned for low-latency redirects', icon: <BoltIcon /> },
                  { label: 'Per-link analytics across browser, OS, device, country, and daily trends', icon: <InsightsIcon /> },
                  { label: 'Rate limiting, environment isolation, OpenAPI docs, and observable services', icon: <SecurityIcon /> },
                ].map((item) => (
                  <Stack key={item.label} direction="row" spacing={1.5} alignItems="center">
                    <Box sx={{ width: 40, height: 40, borderRadius: 3, display: 'grid', placeItems: 'center', backgroundColor: 'rgba(255,255,255,0.12)' }}>
                      {item.icon}
                    </Box>
                    <Typography>{item.label}</Typography>
                  </Stack>
                ))}
              </Stack>
            </Stack>
          </Paper>

          <Paper sx={{ width: { xs: '100%', md: 460 }, p: { xs: 3, md: 4 } }}>
            <Stack spacing={3}>
              <Box>
                <Typography variant="h4">Access the dashboard</Typography>
                <Typography color="text.secondary">
                  Create an account or sign in to manage only your own shortened URLs.
                </Typography>
              </Box>

              <Tabs
                value={mode}
                onChange={(_, value) => {
                  setMode(value)
                  setError(null)
                  setSuccess(null)
                }}
                variant="fullWidth"
              >
                <Tab value="login" label="Login" />
                <Tab value="register" label="Register" />
              </Tabs>

              {success && <Alert severity="success">{success}</Alert>}
              {error && <Alert severity="error">{error}</Alert>}

              <Box component="form" onSubmit={handleSubmit}>
                <Stack spacing={2}>
                  {mode === 'register' && (
                    <TextField
                      label="Full name"
                      value={form.fullName}
                      onChange={(event) => setForm((current) => ({ ...current, fullName: event.target.value }))}
                      required
                    />
                  )}
                  <TextField
                    label="Email"
                    type="email"
                    value={form.email}
                    onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
                    required
                  />
                  <TextField
                    label="Password"
                    type="password"
                    value={form.password}
                    onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
                    required
                  />
                  <Button type="submit" variant="contained" size="large" disabled={loading}>
                    {loading ? 'Working...' : mode === 'login' ? 'Login' : 'Create account'}
                  </Button>
                </Stack>
              </Box>
            </Stack>
          </Paper>
        </Stack>
      </Container>
    </Box>
  )
}
