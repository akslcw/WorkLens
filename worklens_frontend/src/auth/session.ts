import type { RouteRole } from './types'

export const SESSION_STORAGE_KEY = 'worklens-session'

export type AuthSession = {
  token: string
  username: string
  role: RouteRole
  mustChangePassword?: boolean
}

export function readStoredSession() {
  const raw = localStorage.getItem(SESSION_STORAGE_KEY)
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw) as AuthSession
  } catch {
    localStorage.removeItem(SESSION_STORAGE_KEY)
    return null
  }
}

export function persistSession(session: AuthSession) {
  localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session))
}

export function clearSession() {
  localStorage.removeItem(SESSION_STORAGE_KEY)
}

export function resolveHomePath(role: RouteRole) {
  return role === 'MANAGER' ? '/manager' : '/employee'
}

export function resolvePostLoginPath(session: AuthSession) {
  return session.mustChangePassword ? '/change-password' : resolveHomePath(session.role)
}
