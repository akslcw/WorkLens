import { request } from './http'

export type UsageRecord = {
  id: number
  appName: string
  startedAt: string
  endedAt: string
  createdAt: string
}

export type UsageSegment = {
  startedAt: string
  endedAt: string
}

export type UsageAppCard = {
  appName: string
  durationSeconds: number
  segments: UsageSegment[]
}

export type ReportDetailItem = {
  appName: string
  durationSeconds: number
  durationMinutes: number
  ratio: number
}

export type UsageReportView = {
  reportScope: string
  periodType: string
  periodStartDate: string
  periodEndDate: string
  summary: string
  details: ReportDetailItem[]
}

export type UsageView = {
  mode: 'LIVE_USAGE' | 'REPORT'
  date: string
  page?: number
  pageSize?: number
  totalApps?: number
  items?: UsageAppCard[]
  report?: UsageReportView | null
}

export async function getUsageRecords(token: string) {
  return request<UsageRecord[]>('/usage-records', { method: 'GET' }, token)
}

export async function getUsageView(token: string, date: string, page: number, pageSize: number) {
  const query = new URLSearchParams({
    date,
    page: String(page),
    pageSize: String(pageSize),
  })
  return request<UsageView>(`/usage-records/view?${query.toString()}`, { method: 'GET' }, token)
}
