import type {
  AppNotification,
  AuthUser,
  Category,
  CrmUser,
  Customer,
  DashboardActivity,
  DashboardData,
  LoginResponse,
  Page,
  RepairRequest,
  RequestStatus,
  Review,
  Technician,
  UserRole
} from '~/types'

export interface MockRequest {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: Record<string, unknown>
  query?: Record<string, unknown>
}

interface StoredRequest {
  id: number
  customerId: number
  categoryId: number
  description: string
  address: string
  photoUrl?: string
  status: RequestStatus
  technicianId?: number
  createdAt: string
  updatedAt: string
}

function iso(daysAgo: number, hour = 10) {
  const d = new Date()
  d.setDate(d.getDate() - daysAgo)
  d.setHours(hour, 0, 0, 0)
  return d.toISOString()
}

const categories: Category[] = [
  { id: 1, name: 'Smartphone' },
  { id: 2, name: 'Laptop' },
  { id: 3, name: 'Tablet' },
  { id: 4, name: 'TV' },
  { id: 5, name: 'Refrigerator' },
  { id: 6, name: 'Air Conditioner' },
  { id: 7, name: 'Washing Machine' },
  { id: 8, name: 'Microwave Oven' }
]

const technicians: Technician[] = [
  { id: 1, fullName: 'Alex Johnson', phone: '+998 90 123 45 67', active: true },
  { id: 2, fullName: 'Maria Petrova', phone: '+998 91 234 56 78', active: true },
  { id: 3, fullName: 'Dmitry Sokolov', phone: '+998 93 345 67 89', active: true },
  { id: 4, fullName: 'Elena Karpova', phone: '+998 94 456 78 90', active: false }
]

const customers: Customer[] = [
  { id: 1, name: 'Ivan Ivanov', phone: '+998 90 555 01 01', telegramChatId: 101, language: 'ru', createdAt: iso(45) },
  { id: 2, name: 'John Smith', phone: '+998 91 555 02 02', telegramChatId: 102, language: 'en', createdAt: iso(40) },
  { id: 3, name: 'Anna Kim', phone: '+998 92 555 03 03', telegramChatId: 103, language: 'ru', createdAt: iso(35) },
  { id: 4, name: 'Peter Lee', phone: '+998 93 555 04 04', telegramChatId: 104, language: 'en', createdAt: iso(30) },
  { id: 5, name: 'Sara Brown', phone: '+998 94 555 05 05', telegramChatId: 105, language: 'ru', createdAt: iso(20) },
  { id: 6, name: 'Tom Wilson', phone: '+998 90 555 06 06', telegramChatId: 106, language: 'en', createdAt: iso(15) },
  { id: 7, name: 'Nodir Rakhimov', phone: '+998 91 555 07 07', telegramChatId: 107, language: 'uz', createdAt: iso(10) },
  { id: 8, name: 'Kate Anderson', phone: '+998 92 555 08 08', telegramChatId: 108, language: 'en', createdAt: iso(5) }
]

const requests: StoredRequest[] = [
  { id: 101, customerId: 1, categoryId: 1, status: 'COMPLETED', technicianId: 1, createdAt: iso(28, 9), updatedAt: iso(26, 17), description: 'Broken display, phone drops calls.', address: 'Tashkent, Yunusabad, 12' },
  { id: 102, customerId: 2, categoryId: 2, status: 'IN_PROGRESS', technicianId: 2, createdAt: iso(10, 11), updatedAt: iso(2, 15), description: 'Laptop does not turn on after a power surge.', address: 'Tashkent, Chilanzar, 8' },
  { id: 103, customerId: 3, categoryId: 4, status: 'WAITING_PARTS', technicianId: 3, createdAt: iso(8, 14), updatedAt: iso(1, 12), description: 'No picture but sound is present. Backlight issue.', address: 'Tashkent, Mirzo Ulugbek, 21' },
  { id: 104, customerId: 4, categoryId: 5, status: 'NEW', createdAt: iso(1, 9), updatedAt: iso(1, 9), description: 'Not cooling, compressor is buzzing loudly.', address: 'Tashkent, Sergeli, 3' },
  { id: 105, customerId: 5, categoryId: 1, status: 'COMPLETED', technicianId: 1, createdAt: iso(20, 10), updatedAt: iso(19, 16), description: 'Battery replacement, old battery swollen.', address: 'Tashkent, Yakkasaray, 45' },
  { id: 106, customerId: 6, categoryId: 3, status: 'CANCELLED', createdAt: iso(15, 13), updatedAt: iso(14, 10), description: 'Customer decided to buy a new device.', address: 'Tashkent, Yashnabad, 17' },
  { id: 107, customerId: 7, categoryId: 2, status: 'NEW', createdAt: iso(0, 9), updatedAt: iso(0, 9), description: 'Keyboard not working, liquid was spilled.', address: 'Tashkent, Yunusabad, 5' },
  { id: 108, customerId: 8, categoryId: 1, status: 'IN_PROGRESS', technicianId: 2, createdAt: iso(5, 12), updatedAt: iso(3, 18), description: 'Charging port not working, phone does not charge.', address: 'Tashkent, Uchtepa, 9' },
  { id: 109, customerId: 1, categoryId: 6, status: 'COMPLETED', technicianId: 3, createdAt: iso(12, 10), updatedAt: iso(10, 14), description: 'Annual maintenance and refrigerant refill.', address: 'Tashkent, Yunusabad, 12' },
  { id: 110, customerId: 2, categoryId: 5, status: 'WAITING_PARTS', technicianId: 1, createdAt: iso(6, 15), updatedAt: iso(4, 11), description: 'Thermostat replacement needed.', address: 'Tashkent, Chilanzar, 8' },
  { id: 111, customerId: 3, categoryId: 1, status: 'NEW', createdAt: iso(2, 16), updatedAt: iso(2, 16), description: 'Camera focus issue after a drop.', address: 'Tashkent, Mirzo Ulugbek, 21' },
  { id: 112, customerId: 4, categoryId: 2, status: 'COMPLETED', technicianId: 2, createdAt: iso(18, 9), updatedAt: iso(16, 13), description: 'SSD upgrade and system reinstallation.', address: 'Tashkent, Sergeli, 3' },
  { id: 113, customerId: 5, categoryId: 3, status: 'IN_PROGRESS', technicianId: 3, createdAt: iso(4, 11), updatedAt: iso(2, 17), description: 'Touchscreen not responding to touches.', address: 'Tashkent, Yakkasaray, 45' },
  { id: 114, customerId: 6, categoryId: 7, status: 'NEW', createdAt: iso(1, 8), updatedAt: iso(1, 8), description: 'Error code E10, water is not draining.', address: 'Tashkent, Yashnabad, 17' },
  { id: 115, customerId: 7, categoryId: 1, status: 'COMPLETED', technicianId: 1, createdAt: iso(9, 10), updatedAt: iso(7, 12), description: 'Screen replacement, deep crack.', address: 'Tashkent, Yunusabad, 5' }
]

const reviews: Review[] = [
  { id: 1, requestId: 101, customer: 'Ivan Ivanov', rating: 5, comment: 'Fast and professional service.', createdAt: iso(26) },
  { id: 2, requestId: 105, customer: 'Sara Brown', rating: 4, comment: 'Good job, a bit pricey.', createdAt: iso(19) },
  { id: 3, requestId: 109, customer: 'Ivan Ivanov', rating: 5, comment: 'Technician was very polite.', createdAt: iso(10) },
  { id: 4, requestId: 112, customer: 'Peter Lee', rating: 5, comment: 'Upgraded quickly, works great.', createdAt: iso(16) },
  { id: 5, requestId: 115, customer: 'Nodir Rakhimov', rating: 4, comment: 'Screen replaced perfectly.', createdAt: iso(7) }
]

const users: CrmUser[] = [
  { id: 1, fullName: 'Admin Adminov', email: 'admin@example.com', role: 'ADMIN' },
  { id: 2, fullName: 'Manager One', email: 'manager@example.com', role: 'MANAGER' },
  { id: 3, fullName: 'Manager Two', email: 'manager2@example.com', role: 'MANAGER' }
]

const settings: { telegramBotUsername: string } = { telegramBotUsername: 'RepairServiceBot' }

let currentUser: AuthUser = { id: 1, fullName: 'Admin Adminov', email: 'admin@example.com', role: 'ADMIN' }

function categoryById(id?: number) {
  return categories.find(c => c.id === id)
}

function technicianById(id?: number) {
  return technicians.find(t => t.id === id)
}

function customerById(id?: number) {
  return customers.find(c => c.id === id)
}

function toRepairRequest(r: StoredRequest): RepairRequest {
  return {
    id: r.id,
    customer: customerById(r.customerId),
    customerId: r.customerId,
    category: categoryById(r.categoryId),
    categoryId: r.categoryId,
    description: r.description,
    address: r.address,
    photoUrl: r.photoUrl,
    status: r.status,
    technician: r.technicianId != null ? technicianById(r.technicianId) : undefined,
    technicianId: r.technicianId,
    createdAt: r.createdAt,
    updatedAt: r.updatedAt
  }
}

function customersPayload(): Customer[] {
  return customers.map((c) => {
    const list = requests.filter(r => r.customerId === c.id)
    return {
      ...c,
      totalRequests: list.length,
      completedRequests: list.filter(r => r.status === 'COMPLETED').length
    }
  })
}

function techniciansPayload(): Technician[] {
  return technicians.map(t => ({
    ...t,
    currentRequests: requests.filter(r =>
      r.technicianId === t.id && r.status !== 'COMPLETED' && r.status !== 'CANCELLED'
    ).length
  }))
}

function isToday(value: string) {
  const d = new Date(value)
  const now = new Date()
  return d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth() && d.getDate() === now.getDate()
}

function timeAgo(value: string) {
  const mins = Math.max(1, Math.floor((Date.now() - new Date(value).getTime()) / 60000))
  if (mins < 60) return `${mins} min${mins === 1 ? '' : 's'} ago`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours} hour${hours === 1 ? '' : 's'} ago`
  const days = Math.floor(hours / 24)
  return `${days} day${days === 1 ? '' : 's'} ago`
}

function dashboardPayload(): DashboardData {
  const statusCount = (status: RequestStatus) => requests.filter(r => r.status === status).length
  const recentRequests = [...requests]
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    .slice(0, 5)
    .map(toRepairRequest)

  const activity: DashboardActivity[] = requests
    .flatMap((r) => {
      const customer = customerById(r.customerId)
      const entries: DashboardActivity[] = []
      entries.push({
        id: r.id * 10,
        type: 'NEW_REQUEST',
        text: `New request #${r.id} from ${customer?.name ?? 'unknown'}`,
        time: r.createdAt
      })
      if (r.updatedAt !== r.createdAt) {
        entries.push({
          id: r.id * 10 + 1,
          type: 'STATUS_CHANGE',
          text: `Request #${r.id} is now ${r.status.replace('_', ' ').toLowerCase()}`,
          time: r.updatedAt
        })
      }
      return entries
    })
    .sort((a, b) => b.time.localeCompare(a.time))
    .slice(0, 8)

  return {
    totalRequests: requests.length,
    todayRequests: requests.filter(r => isToday(r.createdAt)).length,
    newRequests: statusCount('NEW'),
    inProgress: statusCount('IN_PROGRESS'),
    completed: statusCount('COMPLETED'),
    totalCustomers: customers.length,
    totalTechnicians: technicians.length,
    recentRequests,
    activity
  }
}

function notificationsPayload(): AppNotification[] {
  const items: AppNotification[] = []
  requests.forEach((r) => {
    const customer = customerById(r.customerId)
    items.push({
      id: r.id * 10,
      text: `New request #${r.id} from ${customer?.name ?? 'unknown'}`,
      icon: 'bi-clipboard-plus',
      iconTheme: 'info',
      time: r.createdAt,
      url: `/requests/${r.id}`
    })
    if (r.updatedAt !== r.createdAt) {
      items.push({
        id: r.id * 10 + 1,
        text: `Request #${r.id} is now ${r.status.replace('_', ' ').toLowerCase()}`,
        icon: 'bi-arrow-repeat',
        iconTheme: 'warning',
        time: r.updatedAt,
        url: `/requests/${r.id}`
      })
    }
  })
  items.sort((a, b) => b.time.localeCompare(a.time))
  return items.slice(0, 10).map(n => ({ ...n, time: timeAgo(n.time) }))
}

function nextId(collection: Array<{ id: number }>) {
  return collection.reduce((max, item) => Math.max(max, item.id), 0) + 1
}

function httpError(status: number, message: string) {
  return { statusCode: status, statusMessage: message, data: { message } }
}

export async function mockApi<T>(path: string, options: MockRequest = {}): Promise<T> {
  if (import.meta.client) {
    await new Promise(resolve => setTimeout(resolve, 120))
  }
  const method = (options.method || 'GET').toUpperCase()
  const parts = (path.split('?')[0] ?? '').split('/').filter(Boolean)
  const query = options.query || {}
  const body = options.body || {}

  if (parts[0] === 'auth') {
    if (parts[1] === 'login' && method === 'POST') {
      const email = String(body.email || '').trim()
      const prefix = email.split('@')[0] || 'demo user'
      const fullName = prefix.replace(/[._-]+/g, ' ').replace(/\b\w/g, ch => ch.toUpperCase())
      currentUser = { id: 999, fullName, email: email || 'demo@example.com', role: 'ADMIN' }
      const result: LoginResponse = {
        accessToken: 'mock-access-token',
        refreshToken: 'mock-refresh-token',
        expiresIn: 86400,
        user: currentUser
      }
      return result as T
    }
    if (parts[1] === 'me' && method === 'GET') {
      return { ...currentUser } as T
    }
  }

  if (parts[0] === 'dashboard' && method === 'GET') {
    return dashboardPayload() as T
  }

  if (parts[0] === 'notifications' && method === 'GET') {
    return notificationsPayload() as T
  }

  if (parts[0] === 'settings') {
    if (method === 'GET') return { ...settings } as T
    if (method === 'PUT') {
      if (typeof body.telegramBotUsername === 'string') settings.telegramBotUsername = body.telegramBotUsername
      return { ...settings } as T
    }
  }

  if (parts[0] === 'categories') {
    if (method === 'GET') return [...categories] as T
    if (method === 'POST') {
      const cat: Category = { id: nextId(categories), name: String(body.name || '') }
      categories.push(cat)
      return cat as T
    }
    if (parts[1]) {
      const id = Number(parts[1])
      const cat = categories.find(c => c.id === id)
      if (!cat) throw httpError(404, 'Category not found.')
      if (method === 'PATCH') {
        if (typeof body.name === 'string') cat.name = body.name
        return { ...cat } as T
      }
      if (method === 'DELETE') {
        categories.splice(categories.indexOf(cat), 1)
        return {} as T
      }
    }
  }

  if (parts[0] === 'technicians') {
    if (method === 'GET') return techniciansPayload() as T
    if (method === 'POST') {
      const t: Technician = {
        id: nextId(technicians),
        fullName: String(body.fullName || ''),
        phone: String(body.phone || ''),
        active: true
      }
      technicians.push(t)
      return t as T
    }
    if (parts[1] && parts[2] === 'requests' && method === 'GET') {
      const id = Number(parts[1])
      return requests.filter(r => r.technicianId === id).map(toRepairRequest) as T
    }
    if (parts[1]) {
      const id = Number(parts[1])
      const t = technicians.find(x => x.id === id)
      if (!t) throw httpError(404, 'Technician not found.')
      if (method === 'PATCH') {
        if (typeof body.fullName === 'string') t.fullName = body.fullName
        if (typeof body.phone === 'string') t.phone = body.phone
        if (typeof body.active === 'boolean') t.active = body.active
        return { ...t } as T
      }
      if (method === 'DELETE') {
        technicians.splice(technicians.indexOf(t), 1)
        return {} as T
      }
    }
  }

  if (parts[0] === 'users') {
    if (method === 'GET') return [...users] as T
    if (method === 'POST') {
      const u: CrmUser = {
        id: nextId(users),
        fullName: String(body.fullName || ''),
        email: String(body.email || ''),
        role: (body.role as UserRole) || 'MANAGER'
      }
      users.push(u)
      return u as T
    }
    if (parts[1]) {
      const id = Number(parts[1])
      const u = users.find(x => x.id === id)
      if (!u) throw httpError(404, 'User not found.')
      if (method === 'PATCH') {
        if (typeof body.fullName === 'string') u.fullName = body.fullName
        if (typeof body.email === 'string') u.email = body.email
        if (body.role === 'ADMIN' || body.role === 'MANAGER') u.role = body.role
        return { ...u } as T
      }
      if (method === 'DELETE') {
        users.splice(users.indexOf(u), 1)
        return {} as T
      }
    }
  }

  if (parts[0] === 'customers') {
    if (method === 'GET') return customersPayload() as T
    if (parts[1] && parts[2] === 'requests' && method === 'GET') {
      const id = Number(parts[1])
      return requests.filter(r => r.customerId === id).map(toRepairRequest) as T
    }
    if (parts[1] && method === 'GET') {
      const id = Number(parts[1])
      const c = customersPayload().find(x => x.id === id)
      if (!c) throw httpError(404, 'Customer not found.')
      return c as T
    }
  }

  if (parts[0] === 'requests') {
    if (method === 'GET' && !parts[1]) {
      let list = requests
      const search = String(query.search || '').trim().toLowerCase()
      const status = query.status || ''
      const categoryId = Number(query.categoryId) || 0
      if (search) {
        const customerIds = customers.filter(c => c.name.toLowerCase().includes(search)).map(c => c.id)
        list = list.filter((r) => {
          const req = toRepairRequest(r)
          const categoryName = typeof req.category === 'string' ? req.category : req.category?.name ?? ''
          const text = `${req.customer?.name ?? ''} ${categoryName} ${r.id}`.toLowerCase()
          return text.includes(search) || customerIds.includes(r.customerId)
        })
      }
      if (status) list = list.filter(r => r.status === status)
      if (categoryId) list = list.filter(r => r.categoryId === categoryId)
      const totalElements = list.length
      const page = Math.max(1, Number(query.page) || 1)
      const size = Math.max(1, Number(query.size) || 10)
      const start = (page - 1) * size
      const result: Page<RepairRequest> = {
        content: list.slice(start, start + size).map(toRepairRequest),
        page,
        size,
        totalElements
      }
      return result as T
    }
    if (parts[1] && parts[2] === 'status' && method === 'PATCH') {
      const id = Number(parts[1])
      const r = requests.find(x => x.id === id)
      if (!r) throw httpError(404, 'Request not found.')
      r.status = body.status as RequestStatus
      r.updatedAt = new Date().toISOString()
      return toRepairRequest(r) as T
    }
    if (parts[1] && parts[2] === 'assign' && method === 'PATCH') {
      const id = Number(parts[1])
      const r = requests.find(x => x.id === id)
      if (!r) throw httpError(404, 'Request not found.')
      r.technicianId = Number(body.technicianId) || undefined
      r.updatedAt = new Date().toISOString()
      return toRepairRequest(r) as T
    }
    if (parts[1] && method === 'GET') {
      const id = Number(parts[1])
      const r = requests.find(x => x.id === id)
      if (!r) throw httpError(404, 'Request not found.')
      return toRepairRequest(r) as T
    }
    if (parts[1] && method === 'DELETE') {
      const id = Number(parts[1])
      const r = requests.find(x => x.id === id)
      if (!r) throw httpError(404, 'Request not found.')
      requests.splice(requests.indexOf(r), 1)
      return {} as T
    }
  }

  if (parts[0] === 'reviews' && method === 'GET') {
    return [...reviews] as T
  }

  throw httpError(404, `Mock endpoint not implemented: ${path}`)
}
