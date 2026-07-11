import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import EmployeeAccessRecordsView from './EmployeeAccessRecordsView.vue'

describe('EmployeeAccessRecordsView', () => {
  beforeEach(() => {
    sessionStorage.setItem(
      'worklens-session',
      JSON.stringify({
        token: 'employee-token',
        username: 'employee.alice',
        role: 'EMPLOYEE',
      }),
    )
    vi.restoreAllMocks()
  })

  it('loads requests targeting the current employee with viewing state', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        expect(init?.headers).toMatchObject({
          Authorization: 'Bearer employee-token',
        })

        if (String(input).endsWith('/api/detail-access-requests/targeting-me')) {
          return jsonResponse([
            {
              id: 1,
              requesterEmployeeId: 11,
              requesterEmployeeName: 'Manager Alice',
              reason: 'Security incident follow-up',
              status: 'PENDING',
              createdAt: '2026-07-05T09:00:00',
              processedAt: null,
              hasBeenViewed: false,
              viewedAt: null,
            },
            {
              id: 2,
              requesterEmployeeId: 12,
              requesterEmployeeName: 'Manager Bob',
              reason: 'Approved but not viewed',
              status: 'APPROVED',
              createdAt: '2026-07-05T10:00:00',
              processedAt: '2026-07-05T10:30:00',
              hasBeenViewed: false,
              viewedAt: null,
            },
            {
              id: 3,
              requesterEmployeeId: 13,
              requesterEmployeeName: 'Manager Carol',
              reason: 'Already viewed',
              status: 'USED',
              createdAt: '2026-07-05T11:00:00',
              processedAt: '2026-07-05T11:15:00',
              hasBeenViewed: true,
              viewedAt: '2026-07-05T11:20:00',
            },
            {
              id: 4,
              requesterEmployeeId: 14,
              requesterEmployeeName: 'Manager Dan',
              reason: 'Rejected request',
              status: 'REJECTED',
              createdAt: '2026-07-05T12:00:00',
              processedAt: '2026-07-05T12:15:00',
              hasBeenViewed: false,
              viewedAt: null,
            },
          ])
        }

        return new Response(null, { status: 404 })
      }),
    )

    const wrapper = await mountEmployeeAccessRecords()
    await flushPromises()

    expect(wrapper.text()).toContain('Manager Alice')
    expect(wrapper.text()).toContain('待处理')
    expect(wrapper.text()).toContain('已批准未查看')
    expect(wrapper.text()).toContain('已批准且已查看')
    expect(wrapper.text()).toContain('已拒绝')
  })

  it('approves and rejects pending requests', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input)
        const method = init?.method ?? 'GET'

        if (url.endsWith('/api/detail-access-requests/targeting-me') && method === 'GET') {
          return jsonResponse([
            {
              id: 5,
              requesterEmployeeId: 11,
              requesterEmployeeName: 'Manager Alice',
              reason: 'Pending review',
              status: 'PENDING',
              createdAt: '2026-07-05T09:00:00',
              processedAt: null,
              hasBeenViewed: false,
              viewedAt: null,
            },
            {
              id: 6,
              requesterEmployeeId: 12,
              requesterEmployeeName: 'Manager Bob',
              reason: 'Another pending review',
              status: 'PENDING',
              createdAt: '2026-07-05T10:00:00',
              processedAt: null,
              hasBeenViewed: false,
              viewedAt: null,
            },
          ])
        }

        if (url.endsWith('/api/detail-access-requests/5/decision') && method === 'PATCH') {
          expect(init?.body).toBe(JSON.stringify({ decision: 'APPROVED' }))
          return jsonResponse({
            id: 5,
            requesterEmployeeId: 11,
            targetEmployeeId: 2,
            reason: 'Pending review',
            status: 'APPROVED',
            createdAt: '2026-07-05T09:00:00',
            processedAt: '2026-07-05T09:20:00',
            processedByEmployeeId: 2,
          })
        }

        if (url.endsWith('/api/detail-access-requests/6/decision') && method === 'PATCH') {
          expect(init?.body).toBe(JSON.stringify({ decision: 'REJECTED' }))
          return jsonResponse({
            id: 6,
            requesterEmployeeId: 12,
            targetEmployeeId: 2,
            reason: 'Another pending review',
            status: 'REJECTED',
            createdAt: '2026-07-05T10:00:00',
            processedAt: '2026-07-05T10:20:00',
            processedByEmployeeId: 2,
          })
        }

        return new Response(null, { status: 404 })
      }),
    )

    const wrapper = await mountEmployeeAccessRecords()
    await flushPromises()

    await wrapper.get('[data-test="approve-request-5"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-test="reject-request-6"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('已批准未查看')
    expect(wrapper.text()).toContain('已拒绝')
    expect(wrapper.find('[data-test="approve-request-5"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="reject-request-6"]').exists()).toBe(false)
  })
})

async function mountEmployeeAccessRecords() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/employee', component: { template: '<div />' } },
      { path: '/employee/access-records', component: EmployeeAccessRecordsView },
    ],
  })

  router.push('/employee/access-records')
  await router.isReady()

  return mount(EmployeeAccessRecordsView, {
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
