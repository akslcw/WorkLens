import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App.vue'

describe('App', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.unstubAllGlobals()
  })

  it('logs in as manager and renders the team summary view', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input)
        const method = init?.method ?? 'GET'

        if (url.endsWith('/auth/login') && method === 'POST') {
          return new Response(
            JSON.stringify({
              token: 'manager-token',
              username: 'manager',
              role: 'MANAGER',
            }),
            { status: 200 },
          )
        }

        if (url.endsWith('/auth/me') && method === 'GET') {
          expect(init?.headers).toMatchObject({
            Authorization: 'Bearer manager-token',
          })

          return new Response(
            JSON.stringify({
              employeeId: 1,
              username: 'manager',
              role: 'MANAGER',
            }),
            { status: 200 },
          )
        }

        if (url.endsWith('/team-usage-summary') && method === 'GET') {
          expect(init?.headers).toMatchObject({
            Authorization: 'Bearer manager-token',
          })

          return new Response(
            JSON.stringify({
              teamAverageUsageMinutes: 67.5,
              totalUsageMinutes: 135,
              activeEmployeeCount: 2,
              appUsageRatios: [
                { appName: 'Slack', usageMinutes: 75, usageRatio: 0.5556 },
                { appName: 'Chrome', usageMinutes: 60, usageRatio: 0.4444 },
              ],
            }),
            { status: 200 },
          )
        }

        return new Response(null, { status: 404 })
      }),
    )

    const wrapper = mount(App)

    await wrapper.get('[data-test="username-input"]').setValue('manager')
    await wrapper.get('[data-test="password-input"]').setValue('Password123!')
    await wrapper.get('[data-test="login-form"]').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Team Aggregate View')
    expect(wrapper.text()).toContain('67.5 min')
    expect(wrapper.text()).toContain('135 min')
    expect(wrapper.text()).toContain('Slack')
    expect(wrapper.text()).toContain('55.56%')
  })

  it('logs in as employee and renders the personal usage detail view', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input)
        const method = init?.method ?? 'GET'

        if (url.endsWith('/auth/login') && method === 'POST') {
          return new Response(
            JSON.stringify({
              token: 'employee-token',
              username: 'employee.alice',
              role: 'EMPLOYEE',
            }),
            { status: 200 },
          )
        }

        if (url.endsWith('/auth/me') && method === 'GET') {
          expect(init?.headers).toMatchObject({
            Authorization: 'Bearer employee-token',
          })

          return new Response(
            JSON.stringify({
              employeeId: 2,
              username: 'employee.alice',
              role: 'EMPLOYEE',
            }),
            { status: 200 },
          )
        }

        if (url.endsWith('/usage-records') && method === 'GET') {
          expect(init?.headers).toMatchObject({
            Authorization: 'Bearer employee-token',
          })

          return new Response(
            JSON.stringify([
              {
                id: 1,
                appName: 'Slack',
                startedAt: '2026-07-03T09:00:00',
                endedAt: '2026-07-03T09:30:00',
                createdAt: '2026-07-03T09:31:00',
              },
              {
                id: 2,
                appName: 'Chrome',
                startedAt: '2026-07-03T10:00:00',
                endedAt: '2026-07-03T11:00:00',
                createdAt: '2026-07-03T11:01:00',
              },
            ]),
            { status: 200 },
          )
        }

        return new Response(null, { status: 404 })
      }),
    )

    const wrapper = mount(App)

    await wrapper.get('[data-test="username-input"]').setValue('employee.alice')
    await wrapper.get('[data-test="password-input"]').setValue('Password123!')
    await wrapper.get('[data-test="login-form"]').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Personal Detail View')
    expect(wrapper.text()).toContain('Slack')
    expect(wrapper.text()).toContain('Chrome')
    expect(wrapper.text()).toContain('90 min total')
    expect(wrapper.text()).toContain('2026/07/03')
  })
})
