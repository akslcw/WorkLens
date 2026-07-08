import { request } from './http'

export type ReportHistoryItem = {
  reportType: string
  summary: string
  periodStartedAt: string | null
  periodEndedAt: string | null
  createdAt: string
}

export async function getTeamReportHistory(token: string) {
  return request<ReportHistoryItem[]>('/llm/team-report-history', { method: 'GET' }, token)
}
