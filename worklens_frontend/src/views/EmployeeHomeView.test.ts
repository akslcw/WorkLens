import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import EmployeeHomeView from './EmployeeHomeView.vue'

describe('EmployeeHomeView', () => {
  beforeEach(() => {
    localStorage.setItem(
      'worklens-session',
      JSON.stringify({
        token: 'employee-token',
        username: 'employee.alice',
        role: 'EMPLOYEE',
      }),
    )
    vi.restoreAllMocks()
  })

  it('loads personal usage records and report history', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input)
        expect(init?.headers).toMatchObject({
          Authorization: 'Bearer employee-token',
        })

        if (url.endsWith('/api/usage-records')) {
          return jsonResponse([
            {
              id: 1,
              appName: 'Chrome',
              startedAt: '2026-07-05T09:00:00',
              endedAt: '2026-07-05T10:00:00',
              createdAt: '2026-07-05T10:00:00',
            },
          ])
        }

        if (url.endsWith('/api/llm/employee-report-history')) {
          return jsonResponse([
            {
              reportType: 'EMPLOYEE',
              summary: '你在最近一周上午时段更专注。',
              periodStartedAt: '2026-06-28T00:00:00',
              periodEndedAt: '2026-07-04T23:59:59',
              createdAt: '2026-07-05T12:00:00',
            },
          ])
        }

        return new Response(null, { status: 404 })
      }),
    )

    const wrapper = await mountEmployeeHome()
    await flushPromises()

    expect(wrapper.text()).toContain('Chrome')
    expect(wrapper.text()).toContain('60 min')
    expect(wrapper.text()).toContain('你在最近一周上午时段更专注。')
  })

  it('generates a new weekly report on demand', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input)
        const method = init?.method ?? 'GET'

        if (url.endsWith('/api/usage-records') && method === 'GET') {
          return jsonResponse([])
        }

        if (url.endsWith('/api/llm/employee-report-history') && method === 'GET') {
          return jsonResponse([])
        }

        if (url.endsWith('/api/llm/employee-report') && method === 'POST') {
          return jsonResponse({
            summary: '你本周下午的长时段专注表现更稳定。',
          })
        }

        return new Response(null, { status: 404 })
      }),
    )

    const wrapper = await mountEmployeeHome()
    await flushPromises()

    await wrapper.get('[data-test="generate-employee-report"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-test="current-employee-report"]').text()).toContain(
      '你本周下午的长时段专注表现更稳定。',
    )
  })

  it('shows personal usage records in pages of twenty with load more', async () => {
    const records = Array.from({ length: 25 }, (_, index) => ({
      id: index + 1,
      appName: `App ${index + 1}`,
      startedAt: `2026-07-05T09:${String(index).padStart(2, '0')}:00`,
      endedAt: `2026-07-05T09:${String(index + 1).padStart(2, '0')}:00`,
      createdAt: `2026-07-05T09:${String(index + 1).padStart(2, '0')}:01`,
    }))
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input)
        const method = init?.method ?? 'GET'

        if (url.endsWith('/api/usage-records') && method === 'GET') {
          return jsonResponse(records)
        }

        if (url.endsWith('/api/llm/employee-report-history') && method === 'GET') {
          return jsonResponse([])
        }

        return new Response(null, { status: 404 })
      }),
    )

    const wrapper = await mountEmployeeHome()
    await flushPromises()

    expect(wrapper.findAll('.record-row')).toHaveLength(20)
    expect(wrapper.text()).toContain('App 20')
    expect(wrapper.text()).not.toContain('App 21')

    await wrapper.get('[data-test="load-more-usage-records"]').trigger('click')
    await flushPromises()

    expect(wrapper.findAll('.record-row')).toHaveLength(25)
    expect(wrapper.text()).toContain('App 25')
    expect(wrapper.find('[data-test="load-more-usage-records"]').exists()).toBe(false)
  })
})

async function mountEmployeeHome() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/employee', component: EmployeeHomeView },
      { path: '/employee/access-records', component: { template: '<div />' } },
    ],
  })

  router.push('/employee')
  await router.isReady()

  return mount(EmployeeHomeView, {
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
