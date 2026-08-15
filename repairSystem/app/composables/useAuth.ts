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

  async function fetchProfile() {
    if (accessToken.value) {
      try {
        const profile = await apiFetch<AuthUser>('/me')
        user.value = profile
        if (import.meta.client) {
          if (profile.language) {
            const { setLocale } = useLocale()
            setLocale(profile.language.toLowerCase())
          }
          if (profile.dateFormat) localStorage.setItem('repair_date_format', profile.dateFormat)
          if (profile.timeFormat) localStorage.setItem('repair_time_format', profile.timeFormat)
        }
      } catch (e) {
        void e
      }
    }
  }

  async function init() {
    if (!accessToken.value && refreshToken.value) {
      await refreshSession()
    }
    if (accessToken.value && !user.value) {
      await fetchProfile()
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

  async function updateProfile(data: Record<string, unknown>) {
    const updated = await apiFetch<AuthUser>('/me', {
      method: 'PATCH',
      body: data
    })
    user.value = updated
    return updated
  }

  async function uploadAvatar(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    const result = await apiFetch<NonNullable<AuthUser['avatar']>>('/me/avatar', {
      method: 'PUT',
      body: formData
    })
    if (user.value) user.value.avatar = result
    return result
  }

  async function deleteAvatar() {
    await apiFetch('/me/avatar', { method: 'DELETE' })
    if (user.value) user.value.avatar = null
  }

  async function changePassword(payload: Record<string, string>) {
    await apiFetch('/auth/change-password', {
      method: 'POST',
      body: payload
    })
  }

  return {
    user,
    isAuthenticated,
    login,
    init,
    fetchProfile,
    refreshSession,
    logout,
    updateProfile,
    uploadAvatar,
    deleteAvatar,
    changePassword
  }
}
