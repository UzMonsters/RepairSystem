export type RealtimeEventType
  = | 'REQUEST_CREATED'
    | 'REQUEST_UPDATED'
    | 'REQUEST_ASSIGNED'
    | 'REQUEST_UNASSIGNED'
    | 'REQUEST_STATUS_CHANGED'
    | 'DASHBOARD_INVALIDATED'
    | 'NOTIFICATION_CREATED'
    | 'NOTIFICATION_READ'
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

export interface RequestRealtimePayload {
  requestId?: number
  requestNumber?: string
  customerId?: number
  technicianId?: number
  status?: string
  priority?: string
}

export interface NotificationRealtimePayload {
  notificationId?: number
  notificationType?: string
  targetId?: number
  target?: string
  read?: boolean
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
  participants?: Array<{ type: string, id: number, name?: string }>
  lastMessage?: ChatMessage | null
  updatedAt?: string
}
