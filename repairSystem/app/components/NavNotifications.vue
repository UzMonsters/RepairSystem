<script setup lang="ts">
import type { NotificationSummary } from '~/types'

const props = withDefaults(defineProps<{
  notifications?: NotificationSummary[]
  seeAllUrl?: string
  seeAllText?: string
}>(), {
  notifications: () => [],
  seeAllUrl: '#',
  seeAllText: 'See All Notifications'
})

const { t } = useLocale()
const { user } = useAuth()
const readIds = ref<number[]>([])
const readStorageKey = computed(() => `repair_notification_read_ids:${user.value?.id ?? 'anonymous'}`)
const unreadCount = computed(() => props.notifications.filter(n => !readIds.value.includes(n.id)).length)

onMounted(() => {
  try {
    const stored = localStorage.getItem(readStorageKey.value)
      ?? localStorage.getItem('repair_notification_read_ids')
    readIds.value = JSON.parse(stored || '[]')
  } catch {
    readIds.value = []
  }
})

function markRead(id: number) {
  if (readIds.value.includes(id)) return
  readIds.value = [...readIds.value, id]
  localStorage.setItem(readStorageKey.value, JSON.stringify(readIds.value))
}

function shortKey(value?: string) {
  if (!value) return '-'
  return value.replace(/_/g, ' ').toLowerCase()
}
function timeAgo(value?: string) {
  if (!value) return ''
  const diff = Date.now() - new Date(value).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return t('justNow')
  if (mins < 60) return `${mins}${t('time.minuteShort')}`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}${t('time.hourShort')}`
  return `${Math.floor(hours / 24)}${t('time.dayShort')}`
}
</script>

<template>
  <li class="nav-item dropdown">
    <a
      class="nav-link"
      data-bs-toggle="dropdown"
      href="#"
      @click.prevent
    >
      <i class="bi bi-bell-fill" />
      <span
        v-if="unreadCount"
        class="navbar-badge badge text-bg-warning"
      >{{ unreadCount }}</span>
    </a>
    <div class="dropdown-menu dropdown-menu-lg dropdown-menu-end">
      <span class="dropdown-item dropdown-header">{{ unreadCount }} {{ t('unread') }}</span>
      <template
        v-for="n in props.notifications"
        :key="n.id"
      >
        <div class="dropdown-divider" />
        <NuxtLink
          :to="n.repairRequest?.id ? `/admin/requests/${n.repairRequest.id}` : '/admin/notifications'"
          class="dropdown-item"
          @click="markRead(n.id)"
        >
          <i class="bi bi-bell me-2" />
          <span class="notification-dropdown-title">
            {{ n.title || shortKey(n.type) }}
          </span>
          <span class="notification-dropdown-message text-secondary fs-7">
            <span class="notification-dropdown-message-text">
              {{ n.message || t(`notificationStatus.${n.deliveryStatus}`) }}
            </span>
            <span class="notification-dropdown-time">{{ timeAgo(n.createdAt) }}</span>
          </span>
        </NuxtLink>
      </template>
      <div class="dropdown-divider" />
      <NuxtLink
        :to="seeAllUrl"
        class="dropdown-item dropdown-footer"
      >{{ seeAllText }}</NuxtLink>
    </div>
  </li>
</template>
