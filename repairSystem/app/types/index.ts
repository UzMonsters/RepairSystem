export type RequestStatus
  = | 'NEW'
    | 'ASSIGNED'
    | 'SCHEDULED'
    | 'IN_PROGRESS'
    | 'WAITING_FOR_PARTS'
    | 'COMPLETED'
    | 'CANCELLED'

export type RequestPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'
export type RequestSource = 'ADMIN' | 'TELEGRAM'
export type LanguageCode = 'EN' | 'RU' | 'UZ'
export type UserRole = 'ADMIN' | 'MANAGER'
export type ReviewSource = 'TELEGRAM'

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface AuthUser {
  id: number
  fullName: string
  email: string
  role: UserRole
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType?: string
  accessTokenExpiresIn?: number
  refreshTokenExpiresIn?: number
  user: AuthUser
}

export interface Customer {
  id: number
  fullName: string
  phone: string
  preferredLanguage?: LanguageCode
  registrationSource?: 'ADMIN' | 'TELEGRAM'
  active?: boolean
  telegramLinked?: boolean
  createdAt?: string
  updatedAt?: string
}

export interface Technician {
  id: number
  fullName: string
  phone: string
  specialization?: string
  maximumConcurrentRequests?: number
  preferredLanguage?: LanguageCode
  active?: boolean
  telegramLinked?: boolean
  createdAt?: string
  updatedAt?: string
}

export interface Category {
  id: number
  nameEn: string
  nameRu: string
  nameUz: string
  active?: boolean
  displayOrder?: number
  createdAt?: string
  updatedAt?: string
}

export interface CrmUser {
  id: number
  fullName: string
  email: string
  role: UserRole
}

export interface RepairRequestCustomerSummary {
  id: number
  fullName: string
  phone: string
  preferredLanguage?: LanguageCode
  active?: boolean
}

export interface RepairRequestCategorySummary {
  id: number
  nameEn: string
  nameRu: string
  nameUz: string
  active?: boolean
  displayOrder?: number
}

export interface AssignmentTechnicianSummary {
  id: number
  fullName: string
  phone: string
}

export interface CurrentAssignmentSummary {
  id: number
  repairRequestId: number
  technician: AssignmentTechnicianSummary
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'UNASSIGNED' | 'REASSIGNED' | 'COMPLETED' | 'CANCELLED'
  scheduledVisitAt?: string
  assignedAt?: string
  respondedAt?: string
}

export interface AssignmentDetail {
  id: number
  repairRequestId: number
  technician: AssignmentTechnicianSummary
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'UNASSIGNED' | 'REASSIGNED' | 'COMPLETED' | 'CANCELLED'
  scheduledVisitAt?: string
  assignedBy?: AssignmentUserSummary
  assignedAt?: string
  respondedAt?: string
  rejectionReason?: string
  closureReason?: string
  closedAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface AssignmentUserSummary {
  id: number
  fullName: string
  email: string
  role: UserRole
}

export interface RepairRequestUserSummary {
  id: number
  fullName: string
  email?: string
  role?: UserRole
}

export interface Attachment {
  id: number
  repairRequestId: number
  type: 'CUSTOMER_PROBLEM_PHOTO' | 'DIAGNOSIS_PHOTO' | 'COMPLETION_PHOTO' | 'GENERAL_DOCUMENT'
  originalFileName: string
  contentType?: string
  sizeBytes?: number
  status: string
  uploadedBy?: RepairRequestUserSummary
  uploadedByTechnician?: AssignmentTechnicianSummary
  uploadedAt?: string
}

export interface RepairExecution {
  id?: number
  repairRequestId: number
  startedAt?: string
  startedBy?: RepairRequestUserSummary
  diagnosis?: string
  diagnosisUpdatedAt?: string
  diagnosisUpdatedBy?: RepairRequestUserSummary
  waitingReason?: string
  waitingSince?: string
  workPerformed?: string
  completionNote?: string
  completedAt?: string
  completedBy?: RepairRequestUserSummary
  cancellationReason?: string
  cancelledAt?: string
  cancelledBy?: RepairRequestUserSummary
  createdAt?: string
  updatedAt?: string
}

export interface StatusHistoryItem {
  id: number
  repairRequestId: number
  fromStatus?: RequestStatus
  toStatus: RequestStatus
  reason?: string
  changedBy?: RepairRequestUserSummary
  changedAt?: string
}

export interface RepairRequest {
  id: number
  requestNumber: string
  status: RequestStatus
  priority: RequestPriority
  source: RequestSource
  description?: string
  address?: string
  customerPreferredVisitAt?: string
  customer?: RepairRequestCustomerSummary
  category?: RepairRequestCategorySummary
  currentAssignment?: CurrentAssignmentSummary | null
  createdAt?: string
  updatedAt?: string
}

export interface Review {
  reviewId?: number
  repairRequestId?: number
  requestNumber?: string
  rating: number
  comment?: string
  source?: ReviewSource
  submittedLanguage?: LanguageCode
  submittedAt?: string
  customerId?: number
  customerName?: string
  technicianId?: number
  technicianName?: string
  category?: RepairRequestCategorySummary | null
}

export interface DashboardOverview {
  generatedAt: string
  businessDate: string
  totalRequests: number
  newToday: number
  openRequests: number
  inProgress: number
  waitingForParts: number
  completedToday: number
  completedTotal: number
  cancelledTotal: number
  activeTechnicians: number
  techniciansWithActiveWork: number
  pendingAssignments: number
  averageRating: number | null
  totalReviews: number
}

export type NotificationStatus = 'PENDING' | 'PROCESSING' | 'RETRY_SCHEDULED' | 'DELIVERED' | 'SKIPPED' | 'DEAD'

export interface NotificationSummary {
  id: number
  eventKey?: string
  notificationType?: string
  recipientType?: string
  recipientId?: number
  repairRequestId?: number
  requestNumber?: string
  status: NotificationStatus
  attemptCount: number
  nextAttemptAt?: string
  deliveredAt?: string
  skippedAt?: string
  deadAt?: string
  lastFailureCategory?: string
  createdAt?: string
  updatedAt?: string
}
