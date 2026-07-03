<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getCurrentUser, login, type AuthSession, type CurrentUser } from './api/auth'
import { getTeamUsageSummary, type TeamUsageSummary } from './api/teamUsage'
import { getUsageRecords, type UsageRecord } from './api/usageRecords'

const SESSION_STORAGE_KEY = 'worklens-session'

const credentials = ref({
  username: '',
  password: '',
})

const session = ref<AuthSession | null>(null)
const currentUser = ref<CurrentUser | null>(null)
const teamSummary = ref<TeamUsageSummary | null>(null)
const usageRecords = ref<UsageRecord[]>([])
const loading = ref(false)
const restoring = ref(false)
const loggingIn = ref(false)
const errorMessage = ref('')

const totalPersonalMinutes = computed(() =>
  usageRecords.value.reduce((sum, record) => sum + calculateMinutes(record.startedAt, record.endedAt), 0),
)

const roleLabel = computed(() => {
  if (!currentUser.value) {
    return ''
  }
  return currentUser.value.role === 'MANAGER' ? 'Manager' : 'Employee'
})

onMounted(async () => {
  const storedSession = readStoredSession()
  if (!storedSession) {
    return
  }

  restoring.value = true
  session.value = storedSession
  try {
    await hydrateCurrentView(storedSession, true)
  } finally {
    restoring.value = false
  }
})

async function handleLogin() {
  if (loggingIn.value) {
    return
  }

  loggingIn.value = true
  errorMessage.value = ''

  try {
    const nextSession = await login({
      username: credentials.value.username.trim(),
      password: credentials.value.password,
    })

    session.value = nextSession
    persistSession(nextSession)
    credentials.value.password = ''
    await hydrateCurrentView(nextSession, false)
  } catch (error) {
    errorMessage.value = getErrorMessage(error, 'Login failed.')
  } finally {
    loggingIn.value = false
    restoring.value = false
  }
}

async function handleLogout() {
  session.value = null
  currentUser.value = null
  teamSummary.value = null
  usageRecords.value = []
  errorMessage.value = ''
  credentials.value = {
    username: '',
    password: '',
  }
  localStorage.removeItem(SESSION_STORAGE_KEY)
}

async function hydrateCurrentView(nextSession: AuthSession, silent = false) {
  loading.value = true
  if (!silent) {
    errorMessage.value = ''
  }

  try {
    const me = await getCurrentUser(nextSession.token)
    currentUser.value = me

    if (me.role === 'MANAGER') {
      usageRecords.value = []
      teamSummary.value = await getTeamUsageSummary(nextSession.token)
      return
    }

    teamSummary.value = null
    usageRecords.value = await getUsageRecords(nextSession.token)
  } catch (error) {
    errorMessage.value = getErrorMessage(error, 'Failed to load dashboard data.')
    handleLogout()
  } finally {
    loading.value = false
  }
}

function readStoredSession() {
  const raw = localStorage.getItem(SESSION_STORAGE_KEY)
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw) as AuthSession
  } catch {
    localStorage.removeItem(SESSION_STORAGE_KEY)
    return null
  }
}

function persistSession(nextSession: AuthSession) {
  localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(nextSession))
}

function calculateMinutes(startedAt: string, endedAt: string) {
  const started = new Date(startedAt).getTime()
  const ended = new Date(endedAt).getTime()
  return Math.round((ended - started) / 60000)
}

function formatDateTime(value: string) {
  const date = new Date(value)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}/${month}/${day} ${hour}:${minute}`
}

function formatMinutes(value: number) {
  return `${value} min`
}

function formatRatio(value: number) {
  return `${(value * 100).toFixed(2)}%`
}

function getErrorMessage(error: unknown, fallback: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallback
}
</script>

<template>
  <main class="shell">
    <section class="masthead">
      <div class="brand-column">
        <p class="eyebrow">WorkLens Module 8</p>
        <h1>One login, two cleanly separated views.</h1>
        <p class="summary">
          The frontend only renders what the authenticated role receives from the backend:
          team aggregates for managers, personal details for employees.
        </p>
      </div>

      <div v-if="currentUser" class="identity-panel">
        <span class="identity-label">Signed in as</span>
        <strong>{{ currentUser.username }}</strong>
        <p>{{ roleLabel }} · Employee #{{ currentUser.employeeId }}</p>
        <button class="ghost-button" type="button" @click="handleLogout">Log out</button>
      </div>
    </section>

    <section v-if="restoring" class="status-card">
      <p>Restoring session...</p>
    </section>

    <section v-else-if="!session" class="login-stage">
      <article class="login-card">
        <div class="card-topline">
          <p class="eyebrow">Secure Entry</p>
          <span class="pill">Token-based login</span>
        </div>

        <h2>Sign in to load your backend-backed view.</h2>
        <p class="card-copy">
          Managers land on the aggregate team dashboard. Employees land on their own activity detail page.
        </p>

        <form data-test="login-form" class="login-form" @submit.prevent="handleLogin">
          <label class="field">
            <span>Username</span>
            <input
              data-test="username-input"
              v-model="credentials.username"
              type="text"
              autocomplete="username"
              placeholder="manager or employee.alice"
              required
            />
          </label>

          <label class="field">
            <span>Password</span>
            <input
              data-test="password-input"
              v-model="credentials.password"
              type="password"
              autocomplete="current-password"
              placeholder="Enter your password"
              required
            />
          </label>

          <button class="primary-button" type="submit" :disabled="loggingIn">
            {{ loggingIn ? 'Signing in...' : 'Sign in' }}
          </button>
        </form>

        <p v-if="errorMessage" class="feedback error" role="alert">{{ errorMessage }}</p>
      </article>

      <article class="info-card">
        <div class="info-grid">
          <div>
            <p class="eyebrow">Manager View</p>
            <h3>Team Aggregate View</h3>
            <p>Average usage, active employee count, and application mix without individual drill-down.</p>
          </div>
          <div>
            <p class="eyebrow">Employee View</p>
            <h3>Personal Detail View</h3>
            <p>Only the logged-in employee's own usage timeline and total focus footprint.</p>
          </div>
        </div>
      </article>
    </section>

    <section v-else class="dashboard-stage">
      <p v-if="errorMessage" class="feedback error" role="alert">{{ errorMessage }}</p>
      <p v-else-if="loading" class="status-card">Loading dashboard...</p>

      <template v-else-if="currentUser?.role === 'MANAGER' && teamSummary">
        <article class="view-hero manager-hero">
          <div>
            <p class="eyebrow">Manager Landing</p>
            <h2>Team Aggregate View</h2>
          </div>
          <p class="hero-note">No individual identifiers are rendered in this view.</p>
        </article>

        <section class="metric-grid">
          <article class="metric-card">
            <span class="metric-label">Team Average</span>
            <strong>{{ teamSummary.teamAverageUsageMinutes }} min</strong>
          </article>
          <article class="metric-card">
            <span class="metric-label">Total Usage</span>
            <strong>{{ teamSummary.totalUsageMinutes }} min</strong>
          </article>
          <article class="metric-card">
            <span class="metric-label">Active Employees</span>
            <strong>{{ teamSummary.activeEmployeeCount }}</strong>
          </article>
        </section>

        <article class="detail-card">
          <div class="card-topline">
            <p class="eyebrow">Application Split</p>
            <span class="pill">Aggregate only</span>
          </div>

          <ul class="ratio-list">
            <li v-for="ratio in teamSummary.appUsageRatios" :key="ratio.appName" class="ratio-row">
              <div>
                <strong>{{ ratio.appName }}</strong>
                <p>{{ ratio.usageMinutes }} min</p>
              </div>
              <div class="ratio-meter">
                <span>{{ formatRatio(ratio.usageRatio) }}</span>
                <div class="meter-track">
                  <div class="meter-fill" :style="{ width: `${ratio.usageRatio * 100}%` }"></div>
                </div>
              </div>
            </li>
          </ul>
        </article>
      </template>

      <template v-else-if="currentUser?.role === 'EMPLOYEE'">
        <article class="view-hero employee-hero">
          <div>
            <p class="eyebrow">Employee Landing</p>
            <h2>Personal Detail View</h2>
          </div>
          <p class="hero-note">{{ totalPersonalMinutes }} min total</p>
        </article>

        <article class="detail-card">
          <div class="card-topline">
            <p class="eyebrow">Usage Timeline</p>
            <span class="pill">Private to you</span>
          </div>

          <p v-if="usageRecords.length === 0" class="feedback">No usage records yet.</p>

          <ul v-else class="record-list">
            <li v-for="record in usageRecords" :key="record.id" class="record-row">
              <div class="record-head">
                <strong>{{ record.appName }}</strong>
                <span class="duration-chip">
                  {{ formatMinutes(calculateMinutes(record.startedAt, record.endedAt)) }}
                </span>
              </div>
              <p>{{ formatDateTime(record.startedAt) }} → {{ formatDateTime(record.endedAt) }}</p>
              <small>Captured {{ formatDateTime(record.createdAt) }}</small>
            </li>
          </ul>
        </article>
      </template>
    </section>
  </main>
</template>

<style scoped>
.shell {
  min-height: 100vh;
  padding: 28px;
  background:
    radial-gradient(circle at top left, rgba(255, 240, 211, 0.95), transparent 28%),
    radial-gradient(circle at bottom right, rgba(165, 201, 255, 0.32), transparent 30%),
    linear-gradient(145deg, #f4ede2 0%, #e7edf4 50%, #dce5ef 100%);
}

.masthead,
.login-stage,
.dashboard-stage,
.status-card {
  width: min(1120px, 100%);
  margin: 0 auto;
}

.masthead {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(260px, 0.8fr);
  gap: 24px;
  align-items: stretch;
  margin-bottom: 24px;
}

.brand-column,
.identity-panel,
.login-card,
.info-card,
.view-hero,
.metric-card,
.detail-card,
.status-card {
  border: 1px solid rgba(33, 48, 71, 0.12);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 24px 60px rgba(90, 105, 128, 0.18);
  backdrop-filter: blur(18px);
}

.brand-column {
  padding: 30px 32px;
}

.identity-panel {
  display: grid;
  align-content: start;
  gap: 8px;
  padding: 26px;
  background: linear-gradient(180deg, rgba(25, 35, 52, 0.96), rgba(42, 63, 90, 0.95));
  color: #eef5ff;
}

.identity-panel strong {
  font-family: Georgia, 'Palatino Linotype', serif;
  font-size: 1.45rem;
}

.identity-panel p,
.identity-label {
  margin: 0;
  color: rgba(238, 245, 255, 0.78);
}

.eyebrow {
  margin: 0 0 10px;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 0.74rem;
  color: #9c6231;
}

.brand-column h1,
.login-card h2,
.view-hero h2,
.info-card h3 {
  margin: 0;
  font-family: Georgia, 'Palatino Linotype', serif;
  color: #182132;
  letter-spacing: -0.03em;
}

.brand-column h1 {
  font-size: clamp(2.3rem, 3.8vw, 4.6rem);
  line-height: 0.96;
}

.summary,
.card-copy,
.info-card p,
.hero-note,
.record-row p,
.record-row small,
.ratio-row p {
  margin: 0;
  color: #556274;
  line-height: 1.6;
}

.summary {
  margin-top: 16px;
  max-width: 720px;
}

.login-stage {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(280px, 0.95fr);
  gap: 24px;
}

.login-card,
.info-card,
.detail-card {
  padding: 28px;
}

.card-topline {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
}

.pill,
.duration-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 7px 12px;
  border-radius: 999px;
  background: #e7eef6;
  color: #2f4e6a;
  font-size: 0.82rem;
  letter-spacing: 0.05em;
}

.login-card h2 {
  font-size: 2rem;
}

.card-copy {
  margin-top: 12px;
  margin-bottom: 22px;
}

.login-form {
  display: grid;
  gap: 16px;
}

.field {
  display: grid;
  gap: 8px;
}

.field span {
  color: #24354e;
  font-size: 0.92rem;
}

.field input {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid #ced7e3;
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.96);
  color: #182132;
  font-size: 1rem;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.field input:focus {
  outline: none;
  border-color: #9c6231;
  box-shadow: 0 0 0 4px rgba(156, 98, 49, 0.12);
  transform: translateY(-1px);
}

.primary-button,
.ghost-button {
  border: none;
  border-radius: 999px;
  font-size: 0.96rem;
  cursor: pointer;
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.primary-button {
  padding: 14px 18px;
  background: linear-gradient(135deg, #1d2e47 0%, #345176 55%, #9c6231 100%);
  color: #fbf7f1;
}

.ghost-button {
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.14);
  color: inherit;
  border: 1px solid rgba(255, 255, 255, 0.16);
}

.primary-button:hover,
.ghost-button:hover {
  transform: translateY(-1px);
}

.primary-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.info-grid {
  display: grid;
  gap: 22px;
}

.info-card h3 {
  font-size: 1.5rem;
  margin-bottom: 10px;
}

.status-card {
  padding: 24px 28px;
}

.feedback {
  margin-top: 16px;
  padding: 16px 18px;
  border-radius: 18px;
  background: #edf3f9;
  color: #506072;
}

.feedback.error {
  background: #fff0ee;
  color: #b9382a;
}

.dashboard-stage {
  display: grid;
  gap: 24px;
}

.view-hero {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 18px;
  padding: 28px 30px;
}

.manager-hero {
  background:
    linear-gradient(120deg, rgba(255, 255, 255, 0.86), rgba(247, 234, 220, 0.8)),
    linear-gradient(160deg, #fef6ec 0%, #eff5fb 100%);
}

.employee-hero {
  background:
    linear-gradient(120deg, rgba(255, 255, 255, 0.88), rgba(224, 237, 250, 0.8)),
    linear-gradient(160deg, #f7fbff 0%, #eef2f8 100%);
}

.hero-note {
  font-size: 1rem;
  max-width: 320px;
  text-align: right;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.metric-card {
  padding: 24px;
}

.metric-label {
  display: block;
  margin-bottom: 14px;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: #6e7f93;
  font-size: 0.78rem;
}

.metric-card strong {
  font-family: Georgia, 'Palatino Linotype', serif;
  font-size: clamp(1.9rem, 3vw, 3rem);
  color: #182132;
}

.ratio-list,
.record-list {
  display: grid;
  gap: 14px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.ratio-row,
.record-row {
  display: grid;
  gap: 10px;
  padding: 18px 20px;
  border: 1px solid #dde6f0;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(244, 247, 250, 0.96));
}

.ratio-row {
  grid-template-columns: minmax(0, 0.9fr) minmax(220px, 1.1fr);
  align-items: center;
}

.ratio-row strong,
.record-head strong {
  font-size: 1.06rem;
  color: #182132;
}

.ratio-meter {
  display: grid;
  gap: 8px;
  justify-items: end;
}

.meter-track {
  width: 100%;
  height: 10px;
  border-radius: 999px;
  background: #dbe6f1;
  overflow: hidden;
}

.meter-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #1d2e47 0%, #9c6231 100%);
}

.record-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

@media (max-width: 900px) {
  .shell {
    padding: 18px;
  }

  .masthead,
  .login-stage,
  .metric-grid,
  .ratio-row {
    grid-template-columns: 1fr;
  }

  .view-hero,
  .card-topline,
  .record-head {
    align-items: start;
  }

  .view-hero,
  .card-topline,
  .record-head {
    flex-direction: column;
  }

  .hero-note {
    max-width: none;
    text-align: left;
  }

  .primary-button,
  .ghost-button {
    width: 100%;
  }
}
</style>
