import type { AuthUser } from '~/types'

type TokenResponse = {
  accessTokenExpiresIn?: number
  refreshTokenExpiresIn?: number
  rememberMe?: boolean
}

let refreshPromise: Promise<boolean> | null = null

export function useAuth() {
  const user = useState<AuthUser | null>('auth:user', () => null)
  const avatarObjectUrl = useState<string | null>('auth:avatar-object-url', () => null)
  const isAuthenticated = computed(() => Boolean(user.value))

  function clearAvatarObjectUrl() {
    if (import.meta.client && avatarObjectUrl.value) {
      URL.revokeObjectURL(avatarObjectUrl.value)
    }
    avatarObjectUrl.value = null
  }

  async function loadAvatar() {
    if (!import.meta.client || !user.value?.avatar) {
      clearAvatarObjectUrl()
      return
    }

    try {
      const blob = await apiFetch<Blob>('/me/avatar', {
        responseType: 'blob'
      })
      const imageBlob = new Blob([blob], { type: user.value?.avatar?.contentType || 'image/jpeg' })
      const nextUrl = URL.createObjectURL(imageBlob)
      if (avatarObjectUrl.value) URL.revokeObjectURL(avatarObjectUrl.value)
      avatarObjectUrl.value = nextUrl
    } catch {
      clearAvatarObjectUrl()
    }
  }

  async function refreshSession(): Promise<boolean> {
    if (refreshPromise) return refreshPromise

    refreshPromise = (async () => {
      try {
        const fetcher = import.meta.server ? useRequestFetch() : $fetch
        await fetcher<TokenResponse>('/api/auth/refresh', {
          method: 'POST',
          credentials: 'include'
        })
        return true
      } catch {
        user.value = null
        clearAvatarObjectUrl()
        return false
      } finally {
        refreshPromise = null
      }
    })()

    return refreshPromise
  }

  async function login(email: string, password: string, rememberMe = false) {
    const data = await apiFetch<{ user: AuthUser }>('/auth/login', {
      method: 'POST',
      body: { email, password, rememberMe }
    })
    user.value = data.user
    await loadAvatar()
  }

  async function fetchProfile() {
    if (user.value) return
    try {
      const profile = await apiFetch<AuthUser>('/me')
      user.value = profile
      if (import.meta.client) {
        const storedLocale = localStorage.getItem('repair_lang')
        if (profile.language && !storedLocale) {
          const { setLocale } = useLocale()
          setLocale(profile.language.toLowerCase())
        }
        if (profile.dateFormat) localStorage.setItem('repair_date_format', profile.dateFormat)
        if (profile.timeFormat) localStorage.setItem('repair_time_format', profile.timeFormat)
        await loadAvatar()
      }
    } catch (e) {
      void e
    }
  }

  async function init() {
    if (user.value) return
    try {
      await fetchProfile()
    } catch {
      user.value = null
    }
  }

  async function logout() {
    try {
      await $fetch('/api/auth/logout', {
        method: 'POST',
        credentials: 'include'
      })
    } catch {
      // Local navigation still clears the in-memory profile if the backend is unavailable.
    }
    user.value = null
    clearAvatarObjectUrl()
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
    await loadAvatar()
    return result
  }

  async function deleteAvatar() {
    await apiFetch('/me/avatar', { method: 'DELETE' })
    if (user.value) user.value.avatar = null
    clearAvatarObjectUrl()
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
    avatarObjectUrl,
    loadAvatar,
    refreshSession,
    logout,
    updateProfile,
    uploadAvatar,
    deleteAvatar,
    changePassword
  }
}
