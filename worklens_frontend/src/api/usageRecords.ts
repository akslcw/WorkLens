import { request } from './http'

export type UsageRecord = {
  id: number
  appName: string
  startedAt: string
  endedAt: string
  createdAt: string
}

export async function getUsageRecords(token: string) {
  return request<UsageRecord[]>('/usage-records', { method: 'GET' }, token)
}
