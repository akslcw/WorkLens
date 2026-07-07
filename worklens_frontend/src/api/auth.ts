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
  mustChangePassword?: boolean
}

export type CurrentUser = {
  employeeId: number
  username: string
  role: RouteRole
  mustChangePassword?: boolean
}

export type ChangePasswordPayload = {
  currentPassword: string
  newPassword: string
}

export type ChangePasswordResponse = {
  username: string
  mustChangePassword: boolean
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

export async function changePassword(payload: ChangePasswordPayload, token: string) {
  return request<ChangePasswordResponse>(
    '/auth/change-password',
    {
      method: 'POST',
      body: JSON.stringify(payload),
    },
    token,
  )
}
