import { clearSession } from '../auth/session'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? '/api'
let unauthorizedHandler: (() => void) | undefined

export function setUnauthorizedHandler(handler: (() => void) | undefined) {
  unauthorizedHandler = handler
}

export async function request<T>(path: string, init?: RequestInit, token?: string) {
  const headers: Record<string, string> = {
    ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...toHeaderRecord(init?.headers),
  }

  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...init,
    headers,
  })

  if (response.status === 401) {
    clearSession()
    unauthorizedHandler?.()
  }

  if (!response.ok) {
    const message = await readErrorMessage(response)
    throw new Error(message)
  }

  if (response.status === 204) {
    throw new Error('Expected a JSON response but received no content')
  }

  return (await response.json()) as T
}

export async function requestVoid(path: string, init?: RequestInit, token?: string): Promise<void> {
  const headers: Record<string, string> = {
    ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...toHeaderRecord(init?.headers),
  }
  const response = await fetch(`${apiBaseUrl}${path}`, { ...init, headers })
  if (response.status === 401) {
    clearSession()
    unauthorizedHandler?.()
  }
  if (!response.ok) {
    throw new Error(await readErrorMessage(response))
  }
}

function toHeaderRecord(headers?: HeadersInit) {
  if (!headers) {
    return {}
  }

  if (headers instanceof Headers) {
    return Object.fromEntries(headers.entries())
  }

  if (Array.isArray(headers)) {
    return Object.fromEntries(headers)
  }

  return headers
}

async function readErrorMessage(response: Response) {
  try {
    const contentType = response.headers.get('Content-Type') ?? ''
    if (contentType.includes('application/json')) {
      const data = (await response.json()) as { message?: string; error?: string }
      return data.message ?? data.error ?? `Request failed with status ${response.status}`
    }

    const text = await response.text()
    return text || `Request failed with status ${response.status}`
  } catch {
    return `Request failed with status ${response.status}`
  }
}
