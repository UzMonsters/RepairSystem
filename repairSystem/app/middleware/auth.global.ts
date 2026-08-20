export default defineNuxtRouteMiddleware(async (to) => {
  const { isAuthenticated, init } = useAuth()

  if (to.path === '/' || to.path === '/contacts' || to.path === '/login') return

  if (to.path === '/admin/login') {
    await init()
    if (isAuthenticated.value) return navigateTo('/admin', { replace: true })
    return
  }

  await init()
  if (!isAuthenticated.value) return navigateTo('/admin/login')
})
