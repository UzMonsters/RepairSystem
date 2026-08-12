type ApiMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

type ApiRequest = {
  method?: ApiMethod
  headers?: HeadersInit
  body?: BodyInit | Record<string, unknown> | null
  query?: Record<string, unknown>
  params?: Record<string, unknown>
}

type ErrorPayload = {
  message?: unknown
  code?: unknown
  fieldErrors?: Array<{ message?: unknown }>
}

function findApiPayload(error: unknown): ErrorPayload | undefined {
  const visited = new Set<object>()

  function visit(current: unknown): ErrorPayload | undefined {
    if (!current || typeof current !== 'object') return undefined
    if (visited.has(current)) return undefined
    visited.add(current)

    const record = current as Record<string, unknown>
    for (const key of ['data', 'response', '_data']) {
      const nested = visit(record[key])
      if (nested) return nested
    }

    if (typeof record.message === 'string' || typeof record.code === 'string' || Array.isArray(record.fieldErrors)) {
      return record as ErrorPayload
    }
    return undefined
  }

  return visit(error)
}

export function getApiErrorMessage(error: unknown, fallback: string): string {
  const payload = findApiPayload(error)
  if (typeof payload?.message === 'string' && payload.message.trim()) return payload.message
  const fieldMessage = payload?.fieldErrors?.find(field =>
    typeof field.message === 'string' && field.message.trim())?.message
  return typeof fieldMessage === 'string' ? fieldMessage : fallback
}

export function getApiErrorCode(error: unknown): string | undefined {
  const code = findApiPayload(error)?.code
  return typeof code === 'string' ? code : undefined
}

export function apiFetch<T>(path: string, options: ApiRequest = {}): Promise<T> {
  const { refreshSession, logout } = useAuth()
  const token = useCookie<string | null>('access_token', { default: () => null })
  const headers = new Headers(options.headers as HeadersInit | undefined)
  headers.set('accept', 'application/json')
  if (token.value) headers.set('authorization', `Bearer ${token.value}`)

  const url = `/api${path.startsWith('/') ? path : `/${path}`}`

  async function execute(retried = false): Promise<T> {
    try {
      return await $fetch<T>(url, {
        ...options,
        headers,
        credentials: 'include'
      })
    } catch (error) {
      const status = (error as { status?: number, response?: { status?: number } }).status
        || (error as { response?: { status?: number } }).response?.status
      const isRefreshRequest = path === '/auth/refresh'
      if (status === 401 && !retried && !isRefreshRequest && await refreshSession()) {
        headers.set('authorization', `Bearer ${token.value}`)
        return execute(true)
      }
      if (status === 401 && !isRefreshRequest) await logout()

      // Nuxt wraps the backend error as error.data.data. Promote that payload
      // so existing callers can consistently read error.data.message.
      const err = error as { data?: { data?: unknown } }
      if (err.data?.data && typeof err.data.data === 'object') {
        err.data = err.data.data as { data?: unknown }
      }
      throw err
    }
  }

  return execute()
}
