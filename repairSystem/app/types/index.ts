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
export type UserDateFormat = 'DD_MM_YYYY' | 'DD_SLASH_MM_SLASH_YYYY' | 'YYYY_MM_DD'
export type UserTimeFormat = 'HOUR_24' | 'HOUR_12'
export type UserTheme = 'LIGHT' | 'DARK' | 'SYSTEM'
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
  username: string
  fullName: string
  phone: string | null
  email: string
  role: UserRole
  active: boolean
  avatar: {
    attachmentId: number
    fileName: string
    contentType: string
  } | null
  language: LanguageCode
  dateFormat: UserDateFormat
  timeFormat: UserTimeFormat
  theme: UserTheme
  createdAt: string
  updatedAt: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType?: string
  accessTokenExpiresIn?: number
  refreshTokenExpiresIn?: number
  rememberMe?: boolean
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
  name?: string
  description?: string
  nameEn: string
  nameRu: string
  nameUz: string
  descriptionEn?: string
  descriptionRu?: string
  descriptionUz?: string
  active?: boolean
  createdAt?: string
  updatedAt?: string
}

export interface UserSettings {
  language: LanguageCode
  dateFormat: UserDateFormat
  timeFormat: UserTimeFormat
  theme: UserTheme
}

export interface SystemSettings {
  timezone: string
  defaultLanguage: LanguageCode
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
  name?: string
  description?: string
  nameEn: string
  nameRu: string
  nameUz: string
  active?: boolean
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
  customerFullName?: string
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

export interface DashboardStatusLabel {
  label: string
  labelEn: string
  labelRu: string
  labelUz: string
  en?: string
  ru?: string
  uz?: string
}

export interface DashboardStatusDistributionItem {
  status: RequestStatus
  label: DashboardStatusLabel
  count: number
  percentage: number
}

export interface RequestCategoryDistributionItem {
  categoryId: number
  name: string
  nameEn: string
  nameRu: string
  nameUz: string
  count: number
  percentage: number
}

export type NotificationStatus = 'PENDING' | 'DELIVERED' | 'FAILED' | 'SKIPPED'

export interface NotificationSummary {
  id: number
  type?: string
  recipient?: { type?: string, id?: number, name?: string }
  title?: string
  message?: string
  repairRequest?: { id: number, number?: string } | null
  channel?: string
  deliveryStatus: NotificationStatus
  createdAt?: string
}
