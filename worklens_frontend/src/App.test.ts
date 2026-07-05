import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory } from 'vue-router'
import App from './App.vue'
import { resolveHomePath } from './auth/session'
import type { RouteRole } from './auth/types'
import { createAppRouter } from './router'

const SESSION_STORAGE_KEY = 'worklens-session'

describe('App routing', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.unstubAllGlobals()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('redirects unauthenticated users from business routes to login', async () => {
    const router = createAppRouter(createMemoryHistory())
    router.push('/manager')
    await router.isReady()

    mount(App, {
      global: {
        plugins: [router],
      },
    })

    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe('/login')
  })

  it('routes manager login to the manager home page', async () => {
    stubLoginFetch({
      token: 'manager-token',
      username: 'manager',
      role: 'MANAGER',
    })

    const { router, wrapper } = await mountAppAt('/login')

    await wrapper.get('[data-test="username-input"]').setValue('manager')
    await wrapper.get('[data-test="password-input"]').setValue('Password123!')
    await wrapper.get('[data-test="login-form"]').trigger('submit')
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe('/manager')
    expect(wrapper.text()).toContain('团队视角占位页')
    expect(wrapper.text()).toContain('Manager Home')
    expect(JSON.parse(localStorage.getItem(SESSION_STORAGE_KEY) ?? '{}')).toMatchObject({
      token: 'manager-token',
      role: 'MANAGER',
    })
  })

  it('routes employee login to the employee home page', async () => {
    stubLoginFetch({
      token: 'employee-token',
      username: 'employee.alice',
      role: 'EMPLOYEE',
    })

    const { router, wrapper } = await mountAppAt('/login')

    await wrapper.get('[data-test="username-input"]').setValue('employee.alice')
    await wrapper.get('[data-test="password-input"]').setValue('Password123!')
    await wrapper.get('[data-test="login-form"]').trigger('submit')
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe('/employee')
    expect(wrapper.text()).toContain('个人视角占位页')
    expect(wrapper.text()).toContain('Employee Home')
    expect(JSON.parse(localStorage.getItem(SESSION_STORAGE_KEY) ?? '{}')).toMatchObject({
      token: 'employee-token',
      role: 'EMPLOYEE',
    })
  })

  it('restores an existing manager session and allows protected routes', async () => {
    localStorage.setItem(
      SESSION_STORAGE_KEY,
      JSON.stringify({
        token: 'stored-manager-token',
        username: 'manager',
        role: 'MANAGER',
      }),
    )

    const router = createAppRouter(createMemoryHistory())
    router.push('/manager')
    await router.isReady()

    const wrapper = mount(App, {
      global: {
        plugins: [router],
      },
    })

    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe('/manager')
    expect(wrapper.text()).toContain('团队视角占位页')
  })
})

describe('route role mapping', () => {
  it('maps backend roles to frontend landing pages', () => {
    expect(resolveHomePath('MANAGER')).toBe('/manager')
    expect(resolveHomePath('EMPLOYEE')).toBe('/employee')
  })
})

async function mountAppAt(path: string) {
  const router = createAppRouter(createMemoryHistory())
  router.push(path)
  await router.isReady()

  const wrapper = mount(App, {
    global: {
      plugins: [router],
    },
  })

  return { router, wrapper }
}

function stubLoginFetch(session: { token: string; username: string; role: RouteRole }) {
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input)
      const method = init?.method ?? 'GET'

      if (url.endsWith('/auth/login') && method === 'POST') {
        return new Response(JSON.stringify(session), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        })
      }

      return new Response(null, { status: 404 })
    }),
  )
}
