type ApiMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

type ApiRequest = {
  method?: ApiMethod
  headers?: HeadersInit
  body?: BodyInit | Record<string, unknown> | null
  query?: Record<string, unknown>
  params?: Record<string, unknown>
  responseType?: 'json' | 'blob' | 'arrayBuffer' | 'text'
}

type ErrorPayload = {
  message?: unknown
  code?: unknown
  fieldErrors?: Array<{ field?: unknown, message?: unknown }>
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
  const message = typeof payload?.message === 'string' ? payload.message.trim() : ''
  const fieldMessages = (payload?.fieldErrors ?? [])
    .map((field) => {
      const name = typeof field.field === 'string' ? field.field.trim() : ''
      const detail = typeof field.message === 'string' ? field.message.trim() : ''
      return name && detail ? `${name}: ${detail}` : detail
    })
    .filter(Boolean)
    .filter((value, index, values) => values.indexOf(value) === index)

  if (message && fieldMessages.length) return `${message}\n${fieldMessages.join('\n')}`
  if (message) return message
  if (fieldMessages.length) return fieldMessages.join('\n')
  return fallback
}

export function getApiErrorCode(error: unknown): string | undefined {
  const code = findApiPayload(error)?.code
  return typeof code === 'string' ? code : undefined
}

export function apiFetch<T>(path: string, options: ApiRequest = {}): Promise<T> {
  const { refreshSession, logout, isAuthenticated } = useAuth()
  const fetcher = import.meta.server ? useRequestFetch() : $fetch
  const headers = new Headers(options.headers as HeadersInit | undefined)

  // Attach default accept and language headers
  if (!headers.has('accept')) {
    headers.set('accept', options.responseType === 'blob' ? '*/*' : 'application/json')
  }
  if (!headers.has('accept-language')) {
    const { locale } = useLocale()
    headers.set('accept-language', locale.value || 'uz')
  }

  const url = `/api${path.startsWith('/') ? path : `/${path}`}`

  async function execute(retried = false): Promise<T> {
    try {
      return await fetcher<T>(url, {
        ...options,
        headers,
        credentials: 'include'
      })
    } catch (error) {
      const status = (error as { status?: number, response?: { status?: number } }).status
        || (error as { response?: { status?: number } }).response?.status
      const isAuthRequest = ['/auth/login', '/auth/refresh', '/auth/logout'].includes(path)
      if (status === 401 && !retried && !isAuthRequest && await refreshSession()) {
        return execute(true)
      }
      if (status === 401 && !isAuthRequest && isAuthenticated.value) await logout()

      // Keep FetchError immutable. Error text is normalized by getApiErrorMessage.
      throw error
    }
  }

  return execute()
}
