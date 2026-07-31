import type { AuthUser, LoginResponse } from '~/types'

export function useAuth() {
  const accessToken = useCookie<string | null>('access_token', { default: () => null })
  const refreshToken = useCookie<string | null>('refresh_token', { default: () => null })
  const user = useState<AuthUser | null>('auth:user', () => null)

  const isAuthenticated = computed(() => Boolean(accessToken.value))

  async function login(email: string, password: string) {
    const data = await apiFetch<LoginResponse>('/auth/login', { method: 'POST', body: { email, password } })
    accessToken.value = data.accessToken
    refreshToken.value = data.refreshToken || null
    user.value = data.user
  }

  async function init() {
    if (accessToken.value && !user.value) {
      try {
        user.value = await apiFetch<AuthUser>('/auth/me')
      } catch (e) {
        void e
      }
    }
  }

  async function logout() {
    accessToken.value = null
    refreshToken.value = null
    user.value = null
    await navigateTo('/login')
  }

  return { user, isAuthenticated, login, init, logout }
}
