export default defineNuxtRouteMiddleware(async (to) => {
  const { isAuthenticated, init } = useAuth()

  if (to.path === '/login') return

  await init()
  if (!isAuthenticated.value) return navigateTo('/login')
})
