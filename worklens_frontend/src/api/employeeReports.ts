import { request } from './http'
import type { ReportHistoryItem } from './teamReports'

export type EmployeeReportResponse = {
  summary: string
}

export async function generateEmployeeReport(token: string) {
  return request<EmployeeReportResponse>('/llm/employee-report', { method: 'POST' }, token)
}

export async function getEmployeeReportHistory(token: string) {
  return request<ReportHistoryItem[]>('/llm/employee-report-history', { method: 'GET' }, token)
}
