import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { api } from '../api'
import type { AuthResponse, User } from '../types'

interface AuthContextValue {
  token: string | null
  user: User | null
  login: (payload: AuthResponse) => void
  logout: () => void
  refreshUser: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('shortener.token'))
  const [user, setUser] = useState<User | null>(() => {
    const raw = localStorage.getItem('shortener.user')
    return raw ? JSON.parse(raw) as User : null
  })

  useEffect(() => {
    if (!token) {
      setUser(null)
      return
    }
    void refreshUser()
  }, [token])

  async function refreshUser() {
    if (!localStorage.getItem('shortener.token')) {
      return
    }
    try {
      const { data } = await api.get<User>('/api/auth/me')
      setUser(data)
      localStorage.setItem('shortener.user', JSON.stringify(data))
    } catch {
      logout()
    }
  }

  function login(payload: AuthResponse) {
    localStorage.setItem('shortener.token', payload.accessToken)
    localStorage.setItem('shortener.user', JSON.stringify(payload.user))
    setToken(payload.accessToken)
    setUser(payload.user)
  }

  function logout() {
    localStorage.removeItem('shortener.token')
    localStorage.removeItem('shortener.user')
    setToken(null)
    setUser(null)
  }

  const value = useMemo(() => ({ token, user, login, logout, refreshUser }), [token, user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider')
  }
  return context
}
