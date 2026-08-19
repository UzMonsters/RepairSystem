<script setup lang="ts">
import type { NotificationSummary, Page } from '~/types'
import { getApiErrorMessage } from '~/utils/api'
import { formatDate as formatApiDate } from '~/utils/date'

const { t } = useLocale()
const page = ref(1)
const size = ref(10)
const status = ref('')
const readTab = ref<'all' | 'unread' | 'read'>('all')
const readIds = ref<number[]>([])

const query = computed(() => ({
  page: page.value - 1,
  size: size.value,
  deliveryStatus: status.value || undefined,
  sort: 'createdAt,desc'
}))

const { data, pending, error, refresh } = await useAsyncData('notifications', () =>
  apiFetch<Page<NotificationSummary>>('/notifications', { query: query.value })
)

const errorMessage = computed(() => {
  return getApiErrorMessage(error.value, 'Failed to load notifications.')
})

const errorToast = ref('')
let errorToastTimer: ReturnType<typeof setTimeout> | undefined

watch(error, (value) => {
  if (errorToastTimer) clearTimeout(errorToastTimer)
  errorToast.value = value ? errorMessage.value : ''
  if (errorToast.value) {
    errorToastTimer = setTimeout(() => {
      errorToast.value = ''
    }, 15000)
  }
}, { immediate: true })

onBeforeUnmount(() => {
  if (errorToastTimer) clearTimeout(errorToastTimer)
})

onMounted(() => {
  try {
    readIds.value = JSON.parse(localStorage.getItem('repair_notification_read_ids') || '[]')
  } catch {
    readIds.value = []
  }
})

const rows = computed(() => data.value?.content ?? [])
const visibleRows = computed(() => rows.value.filter((notification) => {
  const isRead = readIds.value.includes(notification.id)
  return readTab.value === 'all' || (readTab.value === 'read' ? isRead : !isRead)
}))
const totalElements = computed(() => data.value?.totalElements ?? 0)
const totalPages = computed(() => data.value?.totalPages ?? 1)

function applyFilters() {
  page.value = 1
  refresh()
}

function goToPage(target: number) {
  page.value = target
  refresh()
}

function changeSize(s: number) {
  size.value = s
  page.value = 1
  refresh()
}

function formatDate(value?: string) {
  return formatApiDate(value, true)
}

function openNotification(notification: NotificationSummary) {
  if (!readIds.value.includes(notification.id)) {
    readIds.value = [...readIds.value, notification.id]
    localStorage.setItem('repair_notification_read_ids', JSON.stringify(readIds.value))
  }
  if (notification.repairRequest?.id) {
    navigateTo(`/admin/requests/${notification.repairRequest.id}`)
  }
}

const statuses = ['PENDING', 'DELIVERED', 'FAILED', 'SKIPPED']
</script>

<template>
  <AppContent
    :title="t('notifications')"
    :breadcrumbs="[{ label: t('home'), to: '/admin' }, { label: t('notifications') }]"
  >
    <Teleport to="body">
      <div
        v-if="errorToast"
        class="notification-error-toast alert alert-danger shadow"
        role="alert"
      >
        <i class="bi bi-exclamation-triangle me-2" />
        {{ errorToast }}
      </div>
    </Teleport>
    <div class="card">
      <div class="card-header">
        <div class="notification-tabs nav nav-pills mb-3">
          <button
            v-for="tab in [
              { value: 'all', label: t('all') },
              { value: 'unread', label: t('unread') },
              { value: 'read', label: t('read') }
            ]"
            :key="tab.value"
            type="button"
            class="nav-link"
            :class="{ active: readTab === tab.value }"
            @click="readTab = tab.value as 'all' | 'unread' | 'read'"
          >
            {{ tab.label }}
          </button>
        </div>
        <div class="d-flex flex-column flex-md-row gap-2 align-items-md-center justify-content-between">
          <h3 class="card-title mb-0">
            {{ t('notifications') }}
          </h3>
          <select
            v-model="status"
            class="form-select form-select-sm"
            style="max-width: 180px;"
            @change="applyFilters"
          >
            <option value="">
              {{ t('all') }}
            </option>
            <option
              v-for="s in statuses"
              :key="s"
              :value="s"
            >
              {{ s }}
            </option>
          </select>
        </div>
      </div>

      <div class="card-body p-0">
        <div
          v-if="error"
          class="alert alert-danger m-3"
        >
          {{ errorMessage }}
          <button
            type="button"
            class="btn btn-sm btn-outline-danger ms-2"
            @click="() => refresh()"
          >
            {{ t('retry') }}
          </button>
        </div>

        <div
          v-else-if="pending"
          class="text-center py-4"
        >
          <div class="spinner-border spinner-border-sm text-primary" />
        </div>
        <div
          v-else-if="!visibleRows.length"
          class="empty-state py-5"
        >
          <i class="bi bi-bell" />
          <p>{{ t('noNotifications') }}</p>
        </div>
        <div
          v-else
          class="notification-list"
        >
          <button
            v-for="n in visibleRows"
            :key="n.id"
            type="button"
            class="notification-list-item w-100 text-start border-0"
            :class="{ 'notification-unread': !readIds.includes(n.id) }"
            @click="openNotification(n)"
          >
            <span class="notification-list-icon rounded-circle">
              <i class="bi bi-bell" />
            </span>
            <span class="notification-list-content">
              <span class="d-flex justify-content-between gap-3">
                <strong>{{ n.title || '-' }}</strong>
                <small class="text-muted text-nowrap">{{ formatDate(n.createdAt) }}</small>
              </span>
              <span class="text-muted d-block">{{ n.message || '-' }}</span>
              <span class="small text-muted">{{ n.recipient?.name || n.recipient?.type || '-' }} · {{ n.channel || '-' }}</span>
            </span>
            <span
              v-if="!readIds.includes(n.id)"
              class="notification-unread-dot"
            />
          </button>
        </div>
      </div>

      <AppPagination
        v-if="!error"
        :page="page"
        :size="size"
        :total="totalElements"
        :total-pages="totalPages"
        @update:page="goToPage"
        @update:size="changeSize"
      />
    </div>
  </AppContent>
</template>
