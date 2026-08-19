import { deleteToken, getMessaging, getToken, onMessage, type Messaging } from 'firebase/messaging'
import { getApps, initializeApp, type FirebaseApp } from 'firebase/app'

type PushConfig = {
  apiKey?: string
  authDomain?: string
  projectId?: string
  storageBucket?: string
  messagingSenderId?: string
  appId?: string
  vapidKey?: string
}

const pushConfig = (): PushConfig => {
  const config = useRuntimeConfig().public
  return {
    apiKey: config.firebaseApiKey,
    authDomain: config.firebaseAuthDomain,
    projectId: config.firebaseProjectId,
    storageBucket: config.firebaseStorageBucket,
    messagingSenderId: config.firebaseMessagingSenderId,
    appId: config.firebaseAppId,
    vapidKey: config.firebaseVapidKey
  }
}

let firebaseApp: FirebaseApp | null = null
let messaging: Messaging | null = null

export function useWebPush() {
  const config = pushConfig()

  const configured = computed(() => Boolean(
    config.apiKey && config.projectId && config.messagingSenderId && config.appId && config.vapidKey
  ))

  async function getMessagingInstance() {
    if (!import.meta.client || !configured.value || !('serviceWorker' in navigator)) return null
    if (!firebaseApp) {
      firebaseApp = getApps()[0] || initializeApp({
        apiKey: config.apiKey,
        authDomain: config.authDomain,
        projectId: config.projectId,
        storageBucket: config.storageBucket,
        messagingSenderId: config.messagingSenderId,
        appId: config.appId
      })
    }
    if (!messaging) messaging = getMessaging(firebaseApp)
    return messaging
  }

  async function register() {
    const instance = await getMessagingInstance()
    if (!instance || !config.vapidKey) return false

    const permission = await Notification.requestPermission()
    if (permission !== 'granted') return false

    await navigator.serviceWorker.register('/firebase-messaging-sw.js')
    const registration = await navigator.serviceWorker.ready
    registration.active?.postMessage({
      type: 'FIREBASE_CONFIG',
      config: {
        apiKey: config.apiKey,
        authDomain: config.authDomain,
        projectId: config.projectId,
        storageBucket: config.storageBucket,
        messagingSenderId: config.messagingSenderId,
        appId: config.appId
      }
    })
    await new Promise<void>((resolve) => {
      const timeout = window.setTimeout(resolve, 1500)
      const onReady = (event: MessageEvent) => {
        if (event.data?.type !== 'FIREBASE_READY') return
        window.clearTimeout(timeout)
        navigator.serviceWorker.removeEventListener('message', onReady)
        resolve()
      }
      navigator.serviceWorker.addEventListener('message', onReady)
    })
    const token = await getToken(instance, { vapidKey: config.vapidKey, serviceWorkerRegistration: registration })
    if (!token) return false

    await apiFetch('/me/push-endpoints', {
      method: 'PUT',
      body: {
        firebaseInstallationId: token,
        clientType: 'ADMIN_WEB',
        platform: 'WEB',
        firebaseAppKey: 'ADMIN_WEB',
        appVersion: '1.0.0'
      }
    })
    return true
  }

  async function unregister() {
    const instance = await getMessagingInstance()
    if (!instance) return
    const token = await getToken(instance, { vapidKey: config.vapidKey })
    if (token) {
      await apiFetch('/me/push-endpoints', {
        method: 'DELETE',
        body: { firebaseInstallationId: token, firebaseAppKey: 'ADMIN_WEB' }
      })
    }
    await deleteToken(instance)
  }

  function listen(onNotification: (payload: unknown) => void) {
    if (!messaging) return () => undefined
    return onMessage(messaging, onNotification)
  }

  return { configured, register, unregister, listen }
}
