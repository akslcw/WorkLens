import { request } from './http'
import type { UsageRecord } from './usageRecords'

export type DetailAccessRequest = {
  id: number
  requesterEmployeeId: number
  targetEmployeeId: number
  reason: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'USED'
  createdAt: string
  processedAt: string | null
  processedByEmployeeId: number | null
}

export type CreateDetailAccessRequestPayload = {
  targetEmployeeId: number
  reason: string
}

export async function listOwnDetailAccessRequests(token: string) {
  return request<DetailAccessRequest[]>('/detail-access-requests', { method: 'GET' }, token)
}

export async function createDetailAccessRequest(payload: CreateDetailAccessRequestPayload, token: string) {
  return request<DetailAccessRequest>(
    '/detail-access-requests',
    {
      method: 'POST',
      body: JSON.stringify(payload),
    },
    token,
  )
}

export async function viewApprovedUsageRecords(requestId: number, token: string) {
  return request<UsageRecord[]>(`/detail-access-requests/${requestId}/usage-records`, { method: 'GET' }, token)
}
