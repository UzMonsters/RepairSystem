import type { RequestStatus } from '~/types'

export const requestStatuses: Array<{ value: RequestStatus, label: string, badge: string }> = [
  { value: 'NEW', label: 'New', badge: 'text-bg-info' },
  { value: 'IN_PROGRESS', label: 'In Progress', badge: 'text-bg-primary' },
  { value: 'WAITING_PARTS', label: 'Waiting for Parts', badge: 'text-bg-warning' },
  { value: 'COMPLETED', label: 'Completed', badge: 'text-bg-success' },
  { value: 'CANCELLED', label: 'Cancelled', badge: 'text-bg-danger' }
]

export function statusMeta(status: RequestStatus) {
  return requestStatuses.find(s => s.value === status) ?? { value: status, label: status, badge: 'text-bg-secondary' }
}
