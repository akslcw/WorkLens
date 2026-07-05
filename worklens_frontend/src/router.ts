import {
  createRouter,
  createWebHistory,
  type RouteLocationNormalized,
  type RouterHistory,
  type RouteRecordRaw,
} from 'vue-router'
import { readStoredSession, resolveHomePath } from './auth/session'
import type { RouteRole } from './auth/types'
import EmployeeHomeView from './views/EmployeeHomeView.vue'
import LoginView from './views/LoginView.vue'
import ManagerHomeView from './views/ManagerHomeView.vue'
import ManagerTeamView from './views/ManagerTeamView.vue'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    role?: RouteRole
    guestOnly?: boolean
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: () => {
      const session = readStoredSession()
      return session ? resolveHomePath(session.role) : '/login'
    },
  },
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: {
      guestOnly: true,
    },
  },
  {
    path: '/manager',
    name: 'manager-home',
    component: ManagerHomeView,
    meta: {
      requiresAuth: true,
      role: 'MANAGER',
    },
  },
  {
    path: '/manager/team',
    name: 'manager-team',
    component: ManagerTeamView,
    meta: {
      requiresAuth: true,
      role: 'MANAGER',
    },
  },
  {
    path: '/employee',
    name: 'employee-home',
    component: EmployeeHomeView,
    meta: {
      requiresAuth: true,
      role: 'EMPLOYEE',
    },
  },
]

export function createAppRouter(history: RouterHistory = createWebHistory()) {
  const router = createRouter({
    history,
    routes,
  })

  router.beforeEach((to) => guardRoute(to))

  return router
}

function guardRoute(to: RouteLocationNormalized) {
  const session = readStoredSession()

  if (to.meta.requiresAuth && !session) {
    return '/login'
  }

  if (to.meta.guestOnly && session) {
    return resolveHomePath(session.role)
  }

  if (to.meta.role && session && to.meta.role !== session.role) {
    return resolveHomePath(session.role)
  }

  return true
}

export type { RouteRole }
