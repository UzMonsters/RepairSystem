import { Client, type IMessage } from '@stomp/stompjs'
import type { RealtimeEvent, RealtimeEventType } from '~/types/realtime'

type RealtimeListener = (event: RealtimeEvent) => void

let client: Client | null = null
let dashboardRefreshTimer: ReturnType<typeof setTimeout> | undefined
const listeners = new Set<RealtimeListener>()

export function useRealtime() {
  const connected = useState<boolean>('realtime:connected', () => false)
  const lastError = useState<string | null>('realtime:last-error', () => null)
  const config = useRuntimeConfig()

  function websocketUrl() {
    if (config.public.realtimeUrl) return config.public.realtimeUrl
    if (!import.meta.client) return ''
    return 'wss://repair-auto.onrender.com/ws'
  }

  function refreshDashboard() {
    if (dashboardRefreshTimer) clearTimeout(dashboardRefreshTimer)
    dashboardRefreshTimer = setTimeout(() => {
      void refreshNuxtData(['dashboard', 'dashboard-recent', 'dashboard-status-counts'])
    }, 400)
  }

  function handleMessage(message: IMessage) {
    try {
      const event = JSON.parse(message.body) as RealtimeEvent
      if (!event?.type) return
      listeners.forEach(listener => listener(event))

      const requestEvents: RealtimeEventType[] = [
        'REQUEST_CREATED',
        'REQUEST_UPDATED',
        'REQUEST_ASSIGNED',
        'REQUEST_ASSIGNMENT_CREATED',
        'REQUEST_ASSIGNMENT_ACCEPTED',
        'REQUEST_ASSIGNMENT_REJECTED',
        'REQUEST_REASSIGNED',
        'REQUEST_UNASSIGNED',
        'REQUEST_SCHEDULE_CHANGED',
        'REQUEST_DIAGNOSIS_UPDATED',
        'REQUEST_ATTACHMENTS_CHANGED',
        'REQUEST_STATUS_CHANGED',
        'REQUEST_DELETED'
      ]
      if (requestEvents.includes(event.type)) {
        void refreshNuxtData(['requests-list', 'dashboard-recent'])
        refreshDashboard()
      }
      if (event.type === 'DASHBOARD_INVALIDATED') refreshDashboard()
    } catch {
      // Ignore malformed broker messages and keep the connection alive.
    }
  }

  async function disconnect() {
    if (dashboardRefreshTimer) clearTimeout(dashboardRefreshTimer)
    dashboardRefreshTimer = undefined
    if (client) {
      await client.deactivate()
      client = null
    }
    connected.value = false
  }

  async function connect(token?: string) {
    if (!import.meta.client || client != null) return
    const url = websocketUrl()
    if (!url) return

    client = new Client({
      brokerURL: url,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => undefined,
      beforeConnect: async () => {
        let currentToken = token
        if (!currentToken) {
          try {
            const res = await $fetch<{ token: string }>('/api/auth/ws-token')
            currentToken = res.token
          } catch {
            // failed to fetch token, will likely fail connect
          }
        }
        if (currentToken && client) {
          client.connectHeaders = { Authorization: `Bearer ${currentToken}` }
        }
      },
      onConnect: () => {
        connected.value = true
        lastError.value = null
        client?.subscribe('/user/queue/events', handleMessage)
        client?.subscribe('/user/queue/chat', handleMessage)
        void refreshNuxtData(['requests-list', 'dashboard', 'dashboard-recent', 'dashboard-status-counts'])
      },
      onDisconnect: () => {
        connected.value = false
      },
      onStompError: (frame) => {
        connected.value = false
        lastError.value = frame.headers.message || 'Realtime connection error'
      },
      onWebSocketClose: () => {
        connected.value = false
      }
    })
    client.activate()
  }

  async function reconnect(token?: string) {
    await disconnect()
    await connect(token)
  }

  function subscribe(listener: RealtimeListener) {
    listeners.add(listener)
    return () => listeners.delete(listener)
  }

  function publish(destination: string, body: Record<string, unknown>) {
    if (!client?.active) return
    client.publish({ destination, body: JSON.stringify(body) })
  }

  return { connected, lastError, connect, disconnect, reconnect, subscribe, publish }
}
