import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import ManagerAccessRequestsView from './ManagerAccessRequestsView.vue'

describe('ManagerAccessRequestsView', () => {
  beforeEach(() => {
    localStorage.setItem(
      'worklens-session',
      JSON.stringify({
        token: 'manager-token',
        username: 'manager',
        role: 'MANAGER',
      }),
    )
    vi.restoreAllMocks()
  })

  it('loads employees and own access requests', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input)
        expect(init?.headers).toMatchObject({
          Authorization: 'Bearer manager-token',
        })

        if (url.endsWith('/api/employees')) {
          return jsonResponse([
            { id: 2, name: 'Alice Chen', employeeNo: 'WL-001', createdAt: '2026-07-05T09:00:00' },
          ])
        }

        if (url.endsWith('/api/detail-access-requests')) {
          return jsonResponse([
            {
              id: 11,
              requesterEmployeeId: 1,
              targetEmployeeId: 2,
              reason: 'Quarterly compliance review',
              status: 'PENDING',
              createdAt: '2026-07-05T10:00:00',
              processedAt: null,
              processedByEmployeeId: null,
            },
          ])
        }

        return new Response(null, { status: 404 })
      }),
    )

    const wrapper = await mountManagerAccessRequests()
    await flushPromises()

    expect(wrapper.text()).toContain('Alice Chen')
    expect(wrapper.text()).toContain('Quarterly compliance review')
    expect(wrapper.text()).toContain('待处理')
  })

  it('creates a new detail access request', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input)
        const method = init?.method ?? 'GET'

        if (url.endsWith('/api/employees') && method === 'GET') {
          return jsonResponse([
            { id: 2, name: 'Alice Chen', employeeNo: 'WL-001', createdAt: '2026-07-05T09:00:00' },
          ])
        }

        if (url.endsWith('/api/detail-access-requests') && method === 'GET') {
          return jsonResponse([])
        }

        if (url.endsWith('/api/detail-access-requests') && method === 'POST') {
          expect(init?.body).toBe(
            JSON.stringify({
              targetEmployeeId: 2,
              reason: 'Need context for a support incident',
            }),
          )
          return jsonResponse(
            {
              id: 12,
              requesterEmployeeId: 1,
              targetEmployeeId: 2,
              reason: 'Need context for a support incident',
              status: 'PENDING',
              createdAt: '2026-07-05T11:00:00',
              processedAt: null,
              processedByEmployeeId: null,
            },
            201,
          )
        }

        return new Response(null, { status: 404 })
      }),
    )

    const wrapper = await mountManagerAccessRequests()
    await flushPromises()

    await wrapper.get('[data-test="target-employee-select"]').setValue('2')
    await wrapper.get('[data-test="access-reason-input"]').setValue('Need context for a support incident')
    await wrapper.get('[data-test="access-request-form"]').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Need context for a support incident')
    expect(wrapper.text()).toContain('待处理')
  })

  it('views an approved request once and disables the action afterwards', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input)
        const method = init?.method ?? 'GET'

        if (url.endsWith('/api/employees') && method === 'GET') {
          return jsonResponse([
            { id: 2, name: 'Alice Chen', employeeNo: 'WL-001', createdAt: '2026-07-05T09:00:00' },
          ])
        }

        if (url.endsWith('/api/detail-access-requests') && method === 'GET') {
          return jsonResponse([
            {
              id: 13,
              requesterEmployeeId: 1,
              targetEmployeeId: 2,
              reason: 'Investigating an incident',
              status: 'APPROVED',
              createdAt: '2026-07-05T09:30:00',
              processedAt: '2026-07-05T09:45:00',
              processedByEmployeeId: 2,
            },
          ])
        }

        if (url.endsWith('/api/detail-access-requests/13/usage-records') && method === 'GET') {
          return jsonResponse([
            {
              id: 101,
              appName: 'Chrome',
              startedAt: '2026-07-05T08:00:00',
              endedAt: '2026-07-05T09:00:00',
              createdAt: '2026-07-05T09:00:00',
            },
          ])
        }

        return new Response(null, { status: 404 })
      }),
    )

    const wrapper = await mountManagerAccessRequests()
    await flushPromises()

    await wrapper.get('[data-test="view-request-13"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-test="request-records-panel"]').text()).toContain('Chrome')
    expect(wrapper.get('[data-test="view-request-13"]').attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('已使用')
  })
})

async function mountManagerAccessRequests() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/manager', component: { template: '<div />' } },
      { path: '/manager/team', component: { template: '<div />' } },
      { path: '/manager/access-requests', component: ManagerAccessRequestsView },
    ],
  })

  router.push('/manager/access-requests')
  await router.isReady()

  return mount(ManagerAccessRequestsView, {
    global: {
      plugins: [router],
    },
  })
}

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}
