import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import ChangePasswordView from './ChangePasswordView.vue'

describe('ChangePasswordView', () => {
  beforeEach(() => {
    localStorage.setItem(
      'worklens-session',
      JSON.stringify({
        token: 'employee-token',
        username: 'E001',
        role: 'EMPLOYEE',
        mustChangePassword: true,
      }),
    )
    vi.restoreAllMocks()
  })

  it('shows the new password once after a successful change before continuing', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        expect(String(input)).toContain('/api/auth/change-password')
        expect(init?.method).toBe('POST')
        expect(init?.headers).toMatchObject({
          Authorization: 'Bearer employee-token',
        })
        expect(init?.body).toBe(
          JSON.stringify({
            currentPassword: 'worklens123',
            newPassword: 'Changed123!',
          }),
        )

        return jsonResponse({
          username: 'E001',
          mustChangePassword: false,
        })
      }),
    )

    const { router, wrapper } = await mountChangePassword()

    await wrapper.get('[data-test="current-password-input"]').setValue('worklens123')
    await wrapper.get('[data-test="new-password-input"]').setValue('Changed123!')
    await wrapper.get('[data-test="change-password-form"]').trigger('submit')
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe('/change-password')
    expect(wrapper.get('[data-test="change-password-success"]').text()).toContain('Changed123!')
    expect(JSON.parse(localStorage.getItem('worklens-session') ?? '{}')).toMatchObject({
      mustChangePassword: false,
    })

    await wrapper.get('[data-test="continue-after-password-change"]').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe('/employee')
  })
})

async function mountChangePassword() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/change-password', component: ChangePasswordView },
      { path: '/employee', component: { template: '<div />' } },
    ],
  })

  router.push('/change-password')
  await router.isReady()

  const wrapper = mount(ChangePasswordView, {
    global: {
      plugins: [router],
    },
  })

  return { router, wrapper }
}

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}
