import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import ManagerTeamView from './ManagerTeamView.vue'

describe('ManagerTeamView', () => {
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

  it('loads team summary and report history for managers', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input)
        expect(init?.headers).toMatchObject({
          Authorization: 'Bearer manager-token',
        })

        if (url.endsWith('/api/team-usage-summary')) {
          return jsonResponse({
            teamAverageUsageMinutes: 87.5,
            totalUsageMinutes: 350,
            activeEmployeeCount: 4,
            appUsageRatios: [
              { appName: 'Chrome', usageMinutes: 160, usageRatio: 0.4571 },
              { appName: 'Slack', usageMinutes: 120, usageRatio: 0.3429 },
            ],
          })
        }

        if (url.endsWith('/api/llm/team-report-history')) {
          return jsonResponse([
            {
              reportType: 'TEAM',
              summary: '过去一周团队沟通工具占比偏高。',
              periodStartedAt: '2026-06-28T00:00:00',
              periodEndedAt: '2026-07-04T23:59:59',
              createdAt: '2026-07-05T08:30:00',
            },
          ])
        }

        return new Response(null, { status: 404 })
      }),
    )

    const wrapper = await mountManagerTeam()
    await flushPromises()

    expect(wrapper.text()).toContain('87.5')
    expect(wrapper.text()).toContain('350')
    expect(wrapper.text()).toContain('Chrome')
    expect(wrapper.text()).toContain('过去一周团队沟通工具占比偏高。')
  })

  it('generates a new team report on demand', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input)
        const method = init?.method ?? 'GET'

        if (url.endsWith('/api/team-usage-summary') && method === 'GET') {
          return jsonResponse({
            teamAverageUsageMinutes: 90,
            totalUsageMinutes: 360,
            activeEmployeeCount: 4,
            appUsageRatios: [],
          })
        }

        if (url.endsWith('/api/llm/team-report-history') && method === 'GET') {
          return jsonResponse([])
        }

        if (url.endsWith('/api/llm/team-report') && method === 'POST') {
          expect(init?.headers).toMatchObject({
            Authorization: 'Bearer manager-token',
          })
          return jsonResponse({
            summary: '团队整体使用节奏稳定，但协作工具时长仍偏高。',
          })
        }

        return new Response(null, { status: 404 })
      }),
    )

    const wrapper = await mountManagerTeam()
    await flushPromises()

    await wrapper.get('[data-test="generate-team-report"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-test="current-team-report"]').text()).toContain(
      '团队整体使用节奏稳定，但协作工具时长仍偏高。',
    )
  })
})

async function mountManagerTeam() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/manager', component: { template: '<div />' } },
      { path: '/manager/team', component: ManagerTeamView },
    ],
  })

  router.push('/manager/team')
  await router.isReady()

  return mount(ManagerTeamView, {
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
