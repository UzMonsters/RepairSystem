type ApiMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

type ApiRequest = {
  method?: ApiMethod
  headers?: HeadersInit
  body?: BodyInit | Record<string, unknown> | null
  query?: Record<string, unknown>
  params?: Record<string, unknown>
}

type ApiErrorLike = {
  message?: string
  statusMessage?: string
  data?: {
    message?: string
    code?: string
    data?: {
      message?: string
      code?: string
    }
  }
}

export function getApiErrorMessage(error: unknown, fallback: string): string {
  const err = error as ApiErrorLike | null | undefined
  return err?.data?.data?.message
    || err?.data?.message
    || err?.message
    || err?.statusMessage
    || fallback
}

export function getApiErrorCode(error: unknown): string | undefined {
  const err = error as ApiErrorLike | null | undefined
  return err?.data?.data?.code || err?.data?.code
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
