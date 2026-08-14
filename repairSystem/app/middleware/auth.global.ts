export default defineNuxtRouteMiddleware(async (to) => {
  const { isAuthenticated, init } = useAuth()

  if (to.path === '/' || to.path === '/contacts' || to.path === '/login' || to.path === '/admin/login') return

  await init()
  if (!isAuthenticated.value) return navigateTo('/admin/login')
})
