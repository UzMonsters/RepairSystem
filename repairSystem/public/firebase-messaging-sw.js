/* Firebase Messaging worker. Runtime config is injected by the page before use. */
importScripts('https://www.gstatic.com/firebasejs/12.17.1/firebase-app-compat.js')
importScripts('https://www.gstatic.com/firebasejs/12.17.1/firebase-messaging-compat.js')

self.addEventListener('message', (event) => {
  if (event.data?.type !== 'FIREBASE_CONFIG') return
  if (!firebase.apps.length) firebase.initializeApp(event.data.config)
  const messaging = firebase.messaging()
  messaging.onBackgroundMessage((payload) => {
    const data = payload.data || {}
    const title = payload.notification?.title || data.title || 'RepairAuto'
    const id = data.repairRequestId || data.requestId
    const url = id ? `/admin/requests/${id}` : '/admin/notifications'
    self.registration.showNotification(title, {
      body: payload.notification?.body || data.message || '',
      data: { url }
    })
  })
  event.source?.postMessage({ type: 'FIREBASE_READY' })
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const url = new URL(event.notification.data?.url || '/admin/notifications', self.location.origin).href
  event.waitUntil(clients.matchAll({ type: 'window', includeUncontrolled: true }).then((windows) => {
    const existing = windows.find((window) => 'focus' in window)
    if (existing) {
      existing.navigate(url)
      return existing.focus()
    }
    return clients.openWindow(url)
  }))
})
