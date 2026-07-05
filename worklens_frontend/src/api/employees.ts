import { request } from './http'

export type Employee = {
  id: number
  name: string
  employeeNo: string
  createdAt: string
}

export type EmployeePayload = {
  name: string
  employeeNo: string
}

export async function getEmployees(token: string) {
  return request<Employee[]>('/employees', { method: 'GET' }, token)
}

export async function createEmployee(payload: EmployeePayload, token: string) {
  return request<Employee>('/employees', {
    method: 'POST',
    body: JSON.stringify(payload),
  }, token)
}

export async function deleteEmployee(id: number, token: string) {
  await request<void>(`/employees/${id}`, {
    method: 'DELETE',
  }, token)
}
