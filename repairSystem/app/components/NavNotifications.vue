<script setup lang="ts">
import type { NotificationSummary } from '~/types'

withDefaults(defineProps<{
  notifications?: NotificationSummary[]
  seeAllUrl?: string
  seeAllText?: string
}>(), {
  notifications: () => [],
  seeAllUrl: '#',
  seeAllText: 'See All Notifications'
})

const { t } = useLocale()

function shortKey(value?: string) {
  if (!value) return '-'
  return value.replace(/_/g, ' ').toLowerCase()
}
function timeAgo(value?: string) {
  if (!value) return ''
  const diff = Date.now() - new Date(value).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return t('justNow')
  if (mins < 60) return `${mins}m`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}h`
  return `${Math.floor(hours / 24)}d`
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
        v-if="notifications.length"
        class="navbar-badge badge text-bg-warning"
      >{{ notifications.length }}</span>
    </a>
    <div class="dropdown-menu dropdown-menu-lg dropdown-menu-end">
      <span class="dropdown-item dropdown-header">{{ notifications.length }} {{ t('notifications') }}</span>
      <template
        v-for="(n, idx) in notifications"
        :key="idx"
      >
        <div class="dropdown-divider" />
        <NuxtLink
          :to="n.repairRequest?.id ? `/admin/requests/${n.repairRequest.id}` : '/admin/notifications'"
          class="dropdown-item"
        >
          <i class="bi bi-bell me-2" />
          <span
            class="d-inline-block"
            style="max-width: 220px;"
          >
            {{ shortKey(n.type) }}
            <span
              v-if="n.repairRequest?.id"
              class="float-end text-secondary fs-7"
            >#{{ n.repairRequest.id }}</span>
          </span>
          <span class="d-block text-secondary fs-7">
            {{ n.deliveryStatus }}
            <span class="float-end">{{ timeAgo(n.createdAt) }}</span>
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
