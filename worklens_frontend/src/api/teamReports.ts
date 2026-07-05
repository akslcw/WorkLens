import { request } from './http'

export type TeamReportResponse = {
  summary: string
}

export type ReportHistoryItem = {
  reportType: string
  summary: string
  periodStartedAt: string | null
  periodEndedAt: string | null
  createdAt: string
}

export async function generateTeamReport(token: string) {
  return request<TeamReportResponse>('/llm/team-report', { method: 'POST' }, token)
}

export async function getTeamReportHistory(token: string) {
  return request<ReportHistoryItem[]>('/llm/team-report-history', { method: 'GET' }, token)
}
