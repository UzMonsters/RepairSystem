import { watchEffect } from 'vue'

export function useTheme() {
  const { user, updateProfile } = useAuth()
  
  const currentTheme = computed(() => {
    return user.value?.theme || 'SYSTEM'
  })
  
  const isDark = computed(() => {
    if (currentTheme.value === 'DARK') return true
    if (currentTheme.value === 'LIGHT') return false
    if (import.meta.client) {
      return window.matchMedia('(prefers-color-scheme: dark)').matches
    }
    return true // Default to dark on server if SYSTEM
  })

  // Watch for changes and apply to body
  if (import.meta.client) {
    watchEffect(() => {
      if (isDark.value) {
        document.documentElement.removeAttribute('data-theme')
      } else {
        document.documentElement.dataset.theme = 'light'
      }
    })
    
    // Listen to system changes if theme is SYSTEM
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
      if (currentTheme.value === 'SYSTEM') {
        if (e.matches) {
          document.documentElement.removeAttribute('data-theme')
        } else {
          document.documentElement.dataset.theme = 'light'
        }
      }
    })
  }

  async function toggleTheme() {
    // If currently dark, switch to light. If currently light, switch to dark.
    // If currently SYSTEM, evaluate current system theme and flip it.
    const newTheme = isDark.value ? 'LIGHT' : 'DARK'
    
    // Immediately apply to local state
    if (user.value) {
      user.value.theme = newTheme
    } else {
      // Temporary fallback if no user
      if (newTheme === 'LIGHT') {
        document.documentElement.dataset.theme = 'light'
      } else {
        document.documentElement.removeAttribute('data-theme')
      }
    }
    
    // Persist to backend if logged in
    if (user.value) {
      try {
        await updateProfile({ theme: newTheme })
      } catch (e) {
        console.error('Failed to save theme preference', e)
      }
    }
  }

  return {
    currentTheme,
    isDark,
    toggleTheme
  }
}
