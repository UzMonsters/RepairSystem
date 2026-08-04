type ApiMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

type ApiRequest = {
  method?: ApiMethod
  headers?: HeadersInit
  body?: BodyInit | Record<string, unknown> | null
  query?: Record<string, unknown>
  params?: Record<string, unknown>
}

export function apiFetch<T>(path: string, options: ApiRequest = {}): Promise<T> {
  const token = useCookie<string | null>('access_token', { default: () => null })
  const headers = new Headers(options.headers as HeadersInit | undefined)
  headers.set('accept', 'application/json')
  if (token.value) headers.set('authorization', `Bearer ${token.value}`)

  return $fetch<T>(`/api${path.startsWith('/') ? path : `/${path}`}`, {
    ...options,
    headers,
    credentials: 'include'
  })
}
