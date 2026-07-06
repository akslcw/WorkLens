<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { generateEmployeeReport, getEmployeeReportHistory } from '../api/employeeReports'
import type { ReportHistoryItem } from '../api/teamReports'
import { getUsageRecords, type UsageRecord } from '../api/usageRecords'
import { clearSession, readStoredSession } from '../auth/session'
import EmployeeWorkspaceNav from '../components/EmployeeWorkspaceNav.vue'

const router = useRouter()
const session = readStoredSession()

const usageRecords = ref<UsageRecord[]>([])
const reportHistory = ref<ReportHistoryItem[]>([])
const currentReport = ref('')
const loading = ref(false)
const generating = ref(false)
const errorMessage = ref('')

onMounted(async () => {
  await loadEmployeeHome()
})

async function loadEmployeeHome() {
  if (!session?.token) {
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const [records, history] = await Promise.all([
      getUsageRecords(session.token),
      getEmployeeReportHistory(session.token),
    ])

    usageRecords.value = records
    reportHistory.value = history
  } catch (error) {
    errorMessage.value = toErrorMessage(error, '个人效率面板加载失败。')
  } finally {
    loading.value = false
  }
}

async function handleGenerateEmployeeReport() {
  if (!session?.token || generating.value) {
    return
  }

  generating.value = true
  errorMessage.value = ''

  try {
    const response = await generateEmployeeReport(session.token)
    currentReport.value = response.summary
    reportHistory.value = [
      {
        reportType: 'EMPLOYEE',
        summary: response.summary,
        periodStartedAt: null,
        periodEndedAt: null,
        createdAt: new Date().toISOString(),
      },
      ...reportHistory.value,
    ]
  } catch (error) {
    errorMessage.value = toErrorMessage(error, '个人周报生成失败。')
  } finally {
    generating.value = false
  }
}

async function handleLogout() {
  clearSession()
  await router.replace('/login')
}

function formatDateTime(value: string | null) {
  if (!value) {
    return '本次即时生成'
  }

  const date = new Date(value)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}/${month}/${day} ${hour}:${minute}`
}

function formatDuration(record: UsageRecord) {
  const startedAt = new Date(record.startedAt).getTime()
  const endedAt = new Date(record.endedAt).getTime()
  return `${Math.round((endedAt - startedAt) / 60000)} min`
}

function reportPeriodLabel(report: ReportHistoryItem) {
  if (!report.periodStartedAt || !report.periodEndedAt) {
    return '最近一周'
  }

  return `${formatDateTime(report.periodStartedAt)} - ${formatDateTime(report.periodEndedAt)}`
}

function toErrorMessage(error: unknown, fallback: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallback
}
</script>

<template>
  <main class="employee-layout">
    <section class="hero-card">
      <div>
        <p class="eyebrow">Employee Workspace</p>
        <h1>个人效率面板</h1>
        <p class="hero-copy">这里只展示当前登录员工自己的使用明细和自己的周报历史，不接受前端传参切换到别人的数据。</p>
        <div class="hero-nav">
          <EmployeeWorkspaceNav current="dashboard" />
        </div>
      </div>
      <div class="hero-actions">
        <div class="session-pill">
          <span>Current User</span>
          <strong>{{ session?.username ?? 'employee' }}</strong>
        </div>
        <button class="ghost-button" type="button" @click="handleLogout">退出登录</button>
      </div>
    </section>

    <p v-if="errorMessage" class="feedback feedback--error" role="alert">{{ errorMessage }}</p>
    <p v-else-if="loading" class="feedback">正在加载你的个人明细与周报历史...</p>

    <template v-else>
      <section class="content-grid">
        <article class="panel-card">
          <div class="panel-head">
            <div>
              <p class="eyebrow">Usage Timeline</p>
              <h2>我的使用明细</h2>
            </div>
            <span class="panel-badge">GET /usage-records</span>
          </div>

          <div v-if="usageRecords.length === 0" class="empty-state">
            <strong>暂无个人使用记录</strong>
            <p>等待桌面采集客户端上报后，这里会显示你自己的应用使用明细。</p>
          </div>

          <ul v-else class="records-list">
            <li v-for="record in usageRecords" :key="record.id" class="record-row">
              <div class="record-main">
                <strong>{{ record.appName }}</strong>
                <p>{{ formatDateTime(record.startedAt) }} - {{ formatDateTime(record.endedAt) }}</p>
                <small>写入时间：{{ formatDateTime(record.createdAt) }}</small>
              </div>
              <span class="duration-chip">{{ formatDuration(record) }}</span>
            </li>
          </ul>
        </article>

        <article class="panel-card">
          <div class="panel-head">
            <div>
              <p class="eyebrow">Weekly Report</p>
              <h2>我的周报</h2>
            </div>
            <span class="panel-badge">POST /llm/employee-report</span>
          </div>

          <button
            data-test="generate-employee-report"
            class="primary-button"
            type="button"
            :disabled="generating"
            @click="handleGenerateEmployeeReport"
          >
            {{ generating ? '生成中...' : '生成我的周报' }}
          </button>

          <div v-if="currentReport" data-test="current-employee-report" class="report-card report-card--current">
            <p class="eyebrow">Current Report</p>
            <p>{{ currentReport }}</p>
          </div>

          <div class="history-head">
            <p class="eyebrow">History</p>
            <span class="panel-badge">GET /llm/employee-report-history</span>
          </div>

          <div v-if="reportHistory.length === 0" class="empty-state">
            <strong>暂无周报历史</strong>
            <p>点击上方按钮后，最近一周的个人周报会出现在这里。</p>
          </div>

          <ul v-else class="history-list">
            <li v-for="(report, index) in reportHistory" :key="`${report.createdAt}-${index}`" class="history-row">
              <strong>{{ report.summary }}</strong>
              <small>生成时间：{{ formatDateTime(report.createdAt) }}</small>
              <small>覆盖时间：{{ reportPeriodLabel(report) }}</small>
            </li>
          </ul>
        </article>
      </section>
    </template>
  </main>
</template>

<style scoped>
.employee-layout {
  position: relative;
  z-index: 1;
  width: min(1180px, calc(100% - 40px));
  min-height: 100vh;
  margin: 0 auto;
  padding: 28px 0 36px;
}

.hero-card,
.panel-card {
  border-radius: 30px;
  border: 1px solid rgba(29, 46, 71, 0.12);
  box-shadow: 0 24px 64px rgba(73, 86, 108, 0.14);
  backdrop-filter: blur(16px);
}

.hero-card {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 20px;
  padding: 30px 32px;
  background:
    radial-gradient(circle at 20% 18%, rgba(109, 145, 189, 0.26), transparent 26%),
    radial-gradient(circle at 88% 24%, rgba(207, 166, 103, 0.18), transparent 30%),
    linear-gradient(145deg, rgba(245, 249, 255, 0.94), rgba(236, 243, 251, 0.92));
}

.eyebrow {
  margin: 0 0 10px;
  color: #4c6d96;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  font-size: 0.76rem;
}

.hero-card h1,
.panel-card h2,
.session-pill strong,
.record-main strong,
.history-row strong,
.empty-state strong {
  margin: 0;
  font-family: Georgia, 'Palatino Linotype', serif;
  letter-spacing: -0.04em;
  color: #1d2e47;
}

.hero-card h1 {
  font-size: clamp(2.4rem, 4vw, 4.6rem);
}

.hero-copy,
.feedback,
.record-main p,
.record-main small,
.report-card p,
.history-row small,
.empty-state p {
  margin: 0;
  line-height: 1.7;
  color: #5d6e83;
}

.hero-copy {
  margin-top: 14px;
  max-width: 620px;
}

.hero-nav {
  margin-top: 18px;
}

.hero-actions {
  display: grid;
  gap: 12px;
  justify-items: end;
}

.session-pill {
  display: grid;
  gap: 4px;
  padding: 14px 16px;
  min-width: 220px;
  border-radius: 22px;
  background: rgba(29, 46, 71, 0.92);
  color: rgba(238, 245, 252, 0.84);
}

.session-pill strong {
  color: #edf4fc;
  font-size: 1.2rem;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 22px;
  margin-top: 22px;
}

.panel-card {
  padding: 24px;
  background: rgba(255, 255, 255, 0.82);
}

.panel-head,
.history-head {
  display: flex;
  justify-content: space-between;
  align-items: start;
  gap: 16px;
}

.panel-head {
  margin-bottom: 20px;
}

.history-head {
  margin-top: 22px;
  margin-bottom: 16px;
}

.panel-head h2 {
  font-size: 2rem;
}

.panel-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: #e8eff7;
  color: #35506b;
  font-size: 0.8rem;
  letter-spacing: 0.08em;
}

.primary-button,
.ghost-button {
  border: none;
  border-radius: 999px;
  cursor: pointer;
  transition: transform 0.2s ease, opacity 0.2s ease;
  font-size: 0.95rem;
}

.primary-button {
  padding: 14px 18px;
  background: linear-gradient(135deg, #1d2e47 0%, #44668e 58%, #b58a56 100%);
  color: #faf7f1;
}

.ghost-button {
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(29, 46, 71, 0.14);
  color: #1d2e47;
}

.primary-button:hover,
.ghost-button:hover {
  transform: translateY(-1px);
}

.primary-button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
}

.feedback {
  margin-top: 18px;
  padding: 15px 16px;
  border-radius: 18px;
  background: #edf3f9;
}

.feedback--error {
  background: #fff0ed;
  color: #b64131;
}

.empty-state {
  display: grid;
  gap: 10px;
  padding: 24px;
  border-radius: 22px;
  border: 1px dashed #cfd8e4;
  background: rgba(244, 247, 251, 0.9);
}

.records-list,
.history-list {
  display: grid;
  gap: 14px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.record-row,
.history-row,
.report-card {
  border-radius: 22px;
  border: 1px solid #dbe4ee;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(244, 247, 250, 0.96));
}

.record-row {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: start;
  padding: 18px 20px;
}

.record-main {
  display: grid;
  gap: 8px;
}

.duration-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 7px 12px;
  border-radius: 999px;
  background: #edf3f9;
  color: #35506b;
  font-size: 0.8rem;
  letter-spacing: 0.06em;
}

.report-card {
  margin-top: 18px;
  padding: 18px 20px;
}

.report-card--current {
  background: linear-gradient(145deg, rgba(247, 251, 255, 0.98), rgba(239, 246, 252, 0.96));
}

.history-row {
  display: grid;
  gap: 8px;
  padding: 18px 20px;
}

@media (max-width: 960px) {
  .employee-layout {
    width: min(100%, calc(100% - 24px));
    padding-top: 18px;
  }

  .hero-card,
  .content-grid,
  .record-row {
    grid-template-columns: 1fr;
  }

  .hero-card,
  .record-row {
    flex-direction: column;
    align-items: start;
  }

  .hero-actions {
    width: 100%;
    justify-items: stretch;
  }

  .ghost-button {
    width: 100%;
  }
}
</style>
