import { request } from './http'

export type AppUsageRatio = {
  appName: string
  usageMinutes: number
  usageRatio: number
}

export type TeamUsageSummary = {
  teamAverageUsageMinutes: number
  totalUsageMinutes: number
  activeEmployeeCount: number
  appUsageRatios: AppUsageRatio[]
}

export async function getTeamUsageSummary(token: string) {
  return request<TeamUsageSummary>('/team-usage-summary', { method: 'GET' }, token)
}
