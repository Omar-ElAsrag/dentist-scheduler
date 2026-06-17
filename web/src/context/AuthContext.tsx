import { createContext, useEffect, useState, type ReactNode } from 'react'
import { supabase } from '../lib/supabase'
import type { User, AuthError } from '@supabase/supabase-js'

type UserRole = 'admin' | 'dentist' | 'receptionist'

interface AuthContextType {
  session: boolean
  user: User | null
  role: UserRole | null
  tenantId: string | null
  fullName: string | null
  login: (email: string, password: string) => Promise<{ error: AuthError | null }>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextType>({
  session: false,
  user: null,
  role: null,
  tenantId: null,
  fullName: null,
  login: async () => ({ error: null }),
  logout: async () => {},
})

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [role, setRole] = useState<UserRole | null>(null)
  const [tenantId, setTenantId] = useState<string | null>(null)
  const [fullName, setFullName] = useState<string | null>(null)

  async function loadProfile(userId: string) {
    const { data } = await supabase
      .from('profiles')
      .select('role, tenant_id, full_name')
      .eq('id', userId)
      .single()

    if (data) {
      setRole(data.role as UserRole)
      setTenantId(data.tenant_id)
      setFullName(data.full_name)
    }
  }

  useEffect(() => {
    supabase.auth.getSession().then(({ data: { session } }) => {
      if (session?.user) {
        setUser(session.user)
        loadProfile(session.user.id)
      }
    })

    const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, session) => {
      if (session?.user) {
        setUser(session.user)
        loadProfile(session.user.id)
      } else {
        setUser(null)
        setRole(null)
        setTenantId(null)
        setFullName(null)
      }
    })

    return () => subscription.unsubscribe()
  }, [])

  async function login(email: string, password: string) {
    const { data, error } = await supabase.auth.signInWithPassword({ email, password })
    if (!error && data.user) {
      setUser(data.user)
      await loadProfile(data.user.id)
    }
    return { error }
  }

  async function logout() {
    await supabase.auth.signOut()
  }

  return (
    <AuthContext.Provider
      value={{
        session: !!user,
        user,
        role,
        tenantId,
        fullName,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}
