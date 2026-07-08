import { request } from './http'
import type { ReportHistoryItem } from './teamReports'

export async function getEmployeeReportHistory(token: string) {
  return request<ReportHistoryItem[]>('/llm/employee-report-history', { method: 'GET' }, token)
}
