export type RealtimeEventType
  // Request Lifecycle & Assignment (13)
  = | 'REQUEST_CREATED'
    | 'REQUEST_UPDATED'
    | 'REQUEST_ASSIGNED'
    | 'REQUEST_ASSIGNMENT_CREATED'
    | 'REQUEST_ASSIGNMENT_ACCEPTED'
    | 'REQUEST_ASSIGNMENT_REJECTED'
    | 'REQUEST_REASSIGNED'
    | 'REQUEST_UNASSIGNED'
    | 'REQUEST_SCHEDULE_CHANGED'
    | 'REQUEST_DIAGNOSIS_UPDATED'
    | 'REQUEST_ATTACHMENTS_CHANGED'
    | 'REQUEST_STATUS_CHANGED'
    | 'REQUEST_DELETED'
  // Analytics & Dashboard (1)
    | 'DASHBOARD_INVALIDATED'
  // Mobile User Notifications (2 - Mobile inboxes only)
    | 'NOTIFICATION_CREATED'
    | 'NOTIFICATION_READ'
  // Realtime Chat (4)
    | 'CHAT_MESSAGE_CREATED'
    | 'CHAT_MESSAGE_READ'
    | 'CHAT_TYPING_STARTED'
    | 'CHAT_TYPING_STOPPED'

export interface RealtimeEvent<T = Record<string, unknown>> {
  eventId: string
  type: RealtimeEventType
  occurredAt: string
  payload: T
}

export interface RequestEventPayload {
  requestId: number
  requestNumber: string | null
  customerId: number | null
  technicianId: number | null
  status: string | null
  oldStatus: string | null
  priority: string | null
}

export interface AssignmentEventPayload {
  requestId: number
  requestNumber: string | null
  assignmentId: number | null
  technicianId: number | null
  previousTechnicianId: number | null
  customerId: number | null
  action: string | null
  status: string | null
}

export interface ScheduleEventPayload {
  requestId: number
  requestNumber: string | null
  assignmentId: number | null
  technicianId: number | null
  customerId: number | null
  scheduledStart: string | null
  scheduledEnd: string | null
  scheduleAction: string | null
}

export interface DiagnosisEventPayload {
  requestId: number
  requestNumber: string | null
  executionId: number | null
  technicianId: number | null
  customerId: number | null
}

export interface AttachmentEventPayload {
  requestId: number
  requestNumber: string | null
  attachmentId: number | null
  changeType: string | null
  customerId: number | null
  technicianId: number | null
}

export interface RequestDeletedPayload {
  requestId: number
  requestNumber: string | null
  customerId: number | null
  technicianId: number | null
}

export interface DashboardInvalidatedPayload {
  reason: string | null
}

export interface ChatMessagePayload {
  messageId: number
  conversationId: number
  senderType: 'CUSTOMER' | 'TECHNICIAN' | 'STAFF'
  senderId: number
  clientMessageId: string | null
  messageType: 'TEXT' | 'IMAGE' | 'FILE'
  text: string | null
  attachmentId: number | null
  replyToMessageId: number | null
  createdAt: string | null
}

export interface ChatReadPayload {
  conversationId: number
  messageId: number
  readerType: 'CUSTOMER' | 'TECHNICIAN' | 'STAFF'
  readerId: number
  readAt: string | null
}

export interface ChatTypingPayload {
  conversationId: number
  actorType: 'CUSTOMER' | 'TECHNICIAN' | 'STAFF'
  actorId: number
  typing: boolean
}

export interface ChatMessage {
  id: number
  conversationId: number
  senderType: string
  senderId: number
  clientMessageId: string
  messageType: 'TEXT' | 'IMAGE' | 'FILE'
  text?: string
  attachmentId?: number | null
  replyToMessageId?: number | null
  createdAt: string
  editedAt?: string | null
  deletedAt?: string | null
}

export interface ConversationSummary {
  id: number
  repairRequestId?: number
  requestNumber?: string
  conversationType: 'CUSTOMER_TECHNICIAN' | 'TECHNICIAN_MANAGER'
  status: 'ACTIVE' | 'CLOSED'
  unreadCount: number
  participants?: Array<{ actorType: string, actorId: number, displayName?: string }>
  lastMessage?: ChatMessage | null
  updatedAt?: string
}
