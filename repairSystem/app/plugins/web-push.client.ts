export default defineNuxtPlugin(() => {
  const { user } = useAuth()
  const push = useWebPush()

  let stopListening: (() => void) | undefined

  type PushPayload = {
    data?: Record<string, string>
    notification?: { title?: string, body?: string, data?: Record<string, string> }
  }

  const openTarget = (payload: PushPayload) => {
    const data = payload?.data || payload?.notification?.data || {}
    const targetId = data.repairRequestId || data.requestId
    const target = data.target
    if (targetId && (target === 'REPAIR_REQUEST_DETAILS' || target === 'TECHNICIAN_JOB_DETAILS')) {
      void navigateTo(`/admin/requests/${targetId}`)
    } else {
      void navigateTo('/admin/notifications')
    }
  }

  watch(user, async (current) => {
    stopListening?.()
    stopListening = undefined
    if (!current || !push.configured.value) return
    try {
      await push.register()
      stopListening = push.listen((payload) => {
        const data = payload as PushPayload
        const title = data?.notification?.title || data?.data?.title || 'RepairAuto'
        if (Notification.permission === 'granted') {
          const notification = new Notification(title, {
            body: data?.notification?.body || data?.data?.message || ''
          })
          notification.onclick = () => openTarget(data)
        }
      })
    } catch {
      // Push is optional and must never block the CRM.
    }
  }, { immediate: true })
})
