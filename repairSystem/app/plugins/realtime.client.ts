export default defineNuxtPlugin(() => {
  const auth = useAuth()
  const realtime = useRealtime()

  watch(auth.isAuthenticated, (authenticated) => {
    if (authenticated) void realtime.connect()
    else void realtime.disconnect()
  }, { immediate: true })
})
