import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { SESSION_STORAGE_KEY } from '../auth/session'
import { request, setUnauthorizedHandler } from './http'

describe('HTTP authentication handling', () => {
  beforeEach(() => {
    sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify({ token: 'expired' }))
  })

  afterEach(() => {
    setUnauthorizedHandler(undefined)
    vi.unstubAllGlobals()
  })

  it('clears the session and invokes the login redirect handler on 401', async () => {
    const unauthorizedHandler = vi.fn()
    setUnauthorizedHandler(unauthorizedHandler)
    vi.stubGlobal('fetch', vi.fn(async () => new Response(
      JSON.stringify({ message: 'Authentication required' }),
      { status: 401, headers: { 'Content-Type': 'application/json' } },
    )))

    await expect(request('/employees', { method: 'GET' }, 'expired'))
      .rejects.toThrow('Authentication required')

    expect(sessionStorage.getItem(SESSION_STORAGE_KEY)).toBeNull()
    expect(unauthorizedHandler).toHaveBeenCalledOnce()
  })
})
