import type { RouteRole } from '../auth/types'
import { request } from './http'

export type LoginPayload = {
  username: string
  password: string
}

export type AuthSession = {
  token: string
  username: string
  role: RouteRole
}

export type CurrentUser = {
  employeeId: number
  username: string
  role: RouteRole
}

export async function login(payload: LoginPayload) {
  return request<AuthSession>(
    '/auth/login',
    {
      method: 'POST',
      body: JSON.stringify(payload),
    },
  )
}

export async function getCurrentUser(token: string) {
  return request<CurrentUser>('/auth/me', { method: 'GET' }, token)
}
