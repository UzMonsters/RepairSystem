import type { AuthUser, LoginResponse } from '~/types'

type TokenResponse = {
  accessToken: string
  refreshToken: string
}

let refreshPromise: Promise<boolean> | null = null

export function useAuth() {
  const accessToken = useCookie<string | null>('access_token', { default: () => null })
  const refreshToken = useCookie<string | null>('refresh_token', { default: () => null })
  const user = useState<AuthUser | null>('auth:user', () => null)

  const isAuthenticated = computed(() => Boolean(accessToken.value))

  async function refreshSession(): Promise<boolean> {
    if (!refreshToken.value) return false
    if (refreshPromise) return refreshPromise

    refreshPromise = (async () => {
      try {
        const data = await $fetch<TokenResponse>('/api/auth/refresh', {
          method: 'POST',
          body: { refreshToken: refreshToken.value },
          credentials: 'include'
        })
        accessToken.value = data.accessToken
        refreshToken.value = data.refreshToken
        return true
      } catch {
        accessToken.value = null
        refreshToken.value = null
        user.value = null
        return false
      } finally {
        refreshPromise = null
      }
    })()

    return refreshPromise
  }

  async function login(email: string, password: string) {
    const data = await apiFetch<LoginResponse>('/auth/login', { method: 'POST', body: { email, password } })
    accessToken.value = data.accessToken
    refreshToken.value = data.refreshToken || null
    user.value = data.user
  }

  async function init() {
    if (!accessToken.value && refreshToken.value) {
      await refreshSession()
    }
    if (accessToken.value && !user.value) {
      try {
        user.value = await apiFetch<AuthUser>('/auth/me')
      } catch (e) {
        void e
      }
    }
  }

  async function logout() {
    const token = refreshToken.value
    if (token) {
      try {
        await $fetch('/api/auth/logout', {
          method: 'POST',
          body: { refreshToken: token },
          credentials: 'include'
        })
      } catch {
        // Local credentials are cleared even if the backend is unavailable.
      }
    }
    accessToken.value = null
    refreshToken.value = null
    user.value = null
    await navigateTo('/admin/login')
  }

  return { user, isAuthenticated, login, init, refreshSession, logout }
}
