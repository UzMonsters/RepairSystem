import type { AuthUser, LoginResponse } from '~/types'

type TokenResponse = {
  accessToken: string
  refreshToken: string
  rememberMe?: boolean
  accessTokenExpiresIn?: number
  refreshTokenExpiresIn?: number
}

let refreshPromise: Promise<boolean> | null = null

export function useAuth() {
  const accessToken = useCookie<string | null>('access_token', { default: () => null })
  // We'll manage refresh_token dynamically via a helper
  const user = useState<AuthUser | null>('auth:user', () => null)
  const avatarObjectUrl = useState<string | null>('auth:avatar-object-url', () => null)
  
  function getRefreshToken() {
    return useCookie<string | null>('refresh_token').value
  }

  function setRefreshTokenCookie(token: string | null, rememberMe?: boolean, maxAgeSeconds?: number) {
    const options = rememberMe
      ? { maxAge: maxAgeSeconds || 30 * 24 * 60 * 60, default: () => null }
      : { maxAge: undefined, default: () => null }
    const cookie = useCookie<string | null>('refresh_token', options)
    cookie.value = token
  }

  const isAuthenticated = computed(() => Boolean(accessToken.value))

  function clearAvatarObjectUrl() {
    if (import.meta.client && avatarObjectUrl.value) {
      URL.revokeObjectURL(avatarObjectUrl.value)
    }
    avatarObjectUrl.value = null
  }

  async function loadAvatar() {
    if (!import.meta.client || !accessToken.value || !user.value?.avatar) {
      clearAvatarObjectUrl()
      return
    }

    try {
      const blob = await $fetch<Blob>('/api/me/avatar', {
        responseType: 'blob',
        headers: { authorization: `Bearer ${accessToken.value}` },
        credentials: 'include'
      })
      const nextUrl = URL.createObjectURL(blob)
      if (avatarObjectUrl.value) URL.revokeObjectURL(avatarObjectUrl.value)
      avatarObjectUrl.value = nextUrl
    } catch {
      clearAvatarObjectUrl()
    }
  }

  async function refreshSession(): Promise<boolean> {
    const currentToken = getRefreshToken()
    if (!currentToken) return false
    if (refreshPromise) return refreshPromise

    refreshPromise = (async () => {
      try {
        const data = await $fetch<TokenResponse>('/api/auth/refresh', {
          method: 'POST',
          body: { refreshToken: currentToken },
          credentials: 'include'
        })
        accessToken.value = data.accessToken
        setRefreshTokenCookie(data.refreshToken, data.rememberMe, data.refreshTokenExpiresIn)
        return true
      } catch {
        accessToken.value = null
        setRefreshTokenCookie(null)
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
    const data = await apiFetch<LoginResponse>('/auth/login', { 
      method: 'POST', 
      body: { email, password, rememberMe } 
    })
    accessToken.value = data.accessToken
    setRefreshTokenCookie(data.refreshToken || null, data.rememberMe, data.refreshTokenExpiresIn)
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
          await loadAvatar()
        }
      } catch (e) {
        void e
      }
    }
  }

  async function init() {
    const currentToken = getRefreshToken()
    if (!accessToken.value && currentToken) {
      await refreshSession()
    }
    if (accessToken.value && !user.value) {
      await fetchProfile()
    }
  }

  async function logout() {
    const token = getRefreshToken()
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
    setRefreshTokenCookie(null)
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
