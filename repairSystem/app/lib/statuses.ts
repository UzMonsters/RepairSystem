import type { RequestPriority, RequestStatus } from '~/types'

export const requestStatuses: Array<{ value: RequestStatus, badge: string }> = [
  { value: 'NEW', badge: 'status-new' },
  { value: 'ASSIGNED', badge: 'status-assigned' },
  { value: 'SCHEDULED', badge: 'status-scheduled' },
  { value: 'IN_PROGRESS', badge: 'status-in-progress' },
  { value: 'WAITING_FOR_PARTS', badge: 'status-waiting' },
  { value: 'COMPLETED', badge: 'status-completed' },
  { value: 'CANCELLED', badge: 'status-cancelled' }
]

export const requestPriorities: Array<{ value: RequestPriority, badge: string }> = [
  { value: 'LOW', badge: 'status-new' },
  { value: 'NORMAL', badge: 'status-assigned' },
  { value: 'HIGH', badge: 'status-waiting' },
  { value: 'URGENT', badge: 'status-cancelled' }
]

export function statusMeta(status: RequestStatus) {
  return requestStatuses.find(s => s.value === status) ?? { value: status, badge: 'status-new' }
}

export function priorityMeta(priority: RequestPriority) {
  return requestPriorities.find(p => p.value === priority) ?? { value: priority, badge: 'status-new' }
}
