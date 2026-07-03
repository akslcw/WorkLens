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

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? '/api'

export async function getEmployees() {
  return request<Employee[]>('/employees')
}

export async function createEmployee(payload: EmployeePayload) {
  return request<Employee>('/employees', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function deleteEmployee(id: number) {
  await request<void>(`/employees/${id}`, {
    method: 'DELETE',
  })
}

async function request<T>(path: string, init?: RequestInit) {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers ?? {}),
    },
    ...init,
  })

  if (!response.ok) {
    throw new Error(`Request failed with status ${response.status}`)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}
