import { request } from './http'

export type LoginPayload = {
  username: string
  password: string
}

export type AuthSession = {
  token: string
  username: string
  role: 'MANAGER' | 'EMPLOYEE'
}

export type CurrentUser = {
  employeeId: number
  username: string
  role: 'MANAGER' | 'EMPLOYEE'
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
