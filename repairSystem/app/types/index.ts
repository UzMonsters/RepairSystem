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
  expiresIn: number
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
  todayRequests: number
  newRequests: number
  inProgress: number
  completed: number
  totalCustomers: number
  totalTechnicians: number
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
}
