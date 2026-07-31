import { mockApi } from '~/lib/mock'
import type { MockRequest } from '~/lib/mock'

export function apiFetch<T>(path: string, options: MockRequest = {}): Promise<T> {
  return mockApi<T>(path, options)
}
