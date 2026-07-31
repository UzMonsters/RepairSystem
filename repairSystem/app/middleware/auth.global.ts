export default defineNuxtRouteMiddleware((to) => {
  const token = useCookie<string | null>('access_token', { default: () => null }).value
  if (to.path === '/login') {
    if (token) return navigateTo('/')
    return
  }
  if (!token) return navigateTo('/login')
})
