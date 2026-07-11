import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import ManagerAccessRequestsView from './ManagerAccessRequestsView.vue'

describe('ManagerAccessRequestsView', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-07-08T12:00:00.000Z'))
    sessionStorage.setItem(
      'worklens-session',
      JSON.stringify({
        token: 'manager-token',
        username: 'manager',
        role: 'MANAGER',
      }),
    )
    vi.restoreAllMocks()
  })

  afterEach(() => {
    vi.useRealTimers()
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
  })

  it('views an approved request as live app cards once and disables the action afterwards', async () => {
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

        if (url.endsWith('/api/detail-access-requests/13/usage-view?date=2026-07-08&page=1&pageSize=10') && method === 'GET') {
          return jsonResponse({
            mode: 'LIVE_USAGE',
            date: '2026-07-08',
            page: 1,
            pageSize: 10,
            totalApps: 1,
            items: [
              {
                appName: 'Chrome',
                durationSeconds: 3600,
                segments: [{ startedAt: '2026-07-08T08:00:00', endedAt: '2026-07-08T09:00:00' }],
              },
            ],
          })
        }

        return new Response(null, { status: 404 })
      }),
    )

    const wrapper = await mountManagerAccessRequests()
    await flushPromises()

    await wrapper.get('[data-test="view-request-13"]').trigger('click')
    await flushPromises()

    const panel = wrapper.get('[data-test="request-usage-view-panel"]')
    expect(panel.text()).toContain('Chrome')
    expect(panel.text()).toContain('60 min')
    expect(panel.text()).toContain('08:00 - 09:00')
    expect(wrapper.get('[data-test="view-request-13"]').attributes('disabled')).toBeDefined()
  })

  it('views an approved historical request as a rolled report', async () => {
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
              id: 14,
              requesterEmployeeId: 1,
              targetEmployeeId: 2,
              reason: 'Historical incident review',
              status: 'APPROVED',
              createdAt: '2026-07-05T09:30:00',
              processedAt: '2026-07-05T09:45:00',
              processedByEmployeeId: 2,
            },
          ])
        }

        if (url.endsWith('/api/detail-access-requests/14/usage-view?date=2026-07-01&page=1&pageSize=10') && method === 'GET') {
          return jsonResponse({
            mode: 'REPORT',
            date: '2026-07-01',
            report: {
              reportScope: 'EMPLOYEE',
              periodType: 'WEEKLY',
              periodStartDate: '2026-06-29',
              periodEndDate: '2026-07-05',
              summary: 'The week was dominated by browser-based work.',
              details: [
                { appName: 'Chrome', durationSeconds: 7200, durationMinutes: 120, ratio: 0.8 },
                { appName: 'Slack', durationSeconds: 1800, durationMinutes: 30, ratio: 0.2 },
              ],
            },
          })
        }

        return new Response(null, { status: 404 })
      }),
    )

    const wrapper = await mountManagerAccessRequests()
    await flushPromises()

    await wrapper.get('[data-test="access-view-date"]').setValue('2026-07-01')
    await wrapper.get('[data-test="view-request-14"]').trigger('click')
    await flushPromises()

    const panel = wrapper.get('[data-test="request-usage-view-panel"]')
    expect(panel.text()).toContain('WEEKLY')
    expect(panel.text()).toContain('The week was dominated by browser-based work.')
    expect(panel.text()).toContain('Chrome')
    expect(panel.text()).toContain('80%')
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
