import { createContext, useContext, useMemo, useState } from 'react'
import { api } from '../lib/api'

type Role = 'CUSTOMER' | 'ADMIN'
type AuthValue = { token: string | null; role: Role | null; login: (email: string, password: string) => Promise<Role | null>; signup: (data: SignupData) => Promise<Role | null>; logout: () => void }
export type SignupData = { email: string; password: string; name: string; phone: string; address: string }
const AuthContext = createContext<AuthValue | null>(null)
const key = 'dacare.accessToken'
function roleOf(token: string | null): Role | null { try { return JSON.parse(atob(token!.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))).role } catch { return null } }

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(key))
  const save = (next: string) => { localStorage.setItem(key, next); setToken(next); return roleOf(next) }
  const value = useMemo<AuthValue>(() => ({ token, role: roleOf(token), login: async (email, password) => save((await api<{ accessToken: string }>('/api/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) })).accessToken), signup: async data => save((await api<{ accessToken: string }>('/api/auth/signup', { method: 'POST', body: JSON.stringify(data) })).accessToken), logout: () => { localStorage.removeItem(key); setToken(null) } }), [token])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
export function useAuth() { const value = useContext(AuthContext); if (!value) throw new Error('AuthProvider가 필요합니다.'); return value }
