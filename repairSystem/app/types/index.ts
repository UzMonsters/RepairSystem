export type RequestStatus = 'NEW' | 'IN_PROGRESS' | 'WAITING_PARTS' | 'COMPLETED' | 'CANCELLED'
export type UserRole = 'ADMIN' | 'MANAGER'

export interface AuthUser {
  id: number
  fullName: string
  email: string
  role: UserRole
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  accessTokenExpiresIn?: number
  expiresIn?: number
  user: AuthUser
}

export interface Customer {
  id: number
  name: string
  phone: string
  telegramChatId?: number
  language?: string
  totalRequests?: number
  completedRequests?: number
  createdAt?: string
}

export interface Technician {
  id: number
  fullName: string
  phone: string
  active?: boolean
  currentRequests?: number
}

export interface Category {
  id: number
  name: string
}

export interface RepairRequest {
  id: number
  customer?: Customer
  customerId?: number
  category?: string | Category
  categoryId?: number
  description?: string
  address?: string
  photoUrl?: string
  status: RequestStatus
  technician?: Technician
  technicianId?: number
  createdAt?: string
  updatedAt?: string
}

export interface Review {
  id?: number
  requestId?: number
  customer?: string
  customerName?: string
  rating: number
  comment?: string
  createdAt?: string
}

export interface CrmUser {
  id: number
  fullName: string
  email: string
  role: UserRole
}

export interface DashboardStats {
  totalRequests: number
  todayRequests: number
  newRequests: number
  inProgress: number
  completed: number
  totalCustomers: number
  totalTechnicians: number
}

export interface DashboardActivity {
  id: number
  type: 'NEW_REQUEST' | 'STATUS_CHANGE' | 'TECHNICIAN_ASSIGNED' | 'REVIEW'
  text: string
  time: string
}

export interface DashboardData extends DashboardStats {
  recentRequests: RepairRequest[]
  activity: DashboardActivity[]
}

export interface AppNotification {
  id: number
  text: string
  icon: string
  iconTheme?: string
  time: string
  url?: string
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
}
