<script setup lang="ts">
import type { NotificationSummary, Page } from '~/types'
import { getApiErrorMessage } from '~/utils/api'
import { formatDate as formatApiDate } from '~/utils/date'

const { t } = useLocale()
const page = ref(1)
const size = ref(10)
const status = ref('')
const sortField = ref('createdAt')
const sortDirection = ref<'asc' | 'desc'>('desc')

const query = computed(() => ({
  page: page.value - 1,
  size: size.value,
  deliveryStatus: status.value || undefined,
  sort: `${sortField.value},${sortDirection.value}`
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

const rows = computed(() => data.value?.content ?? [])
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

function toggleSort(field: string) {
  if (sortField.value === field) sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
  else {
    sortField.value = field
    sortDirection.value = 'asc'
  }
  page.value = 1
  refresh()
}

function sortIcon(field: string) {
  return sortField.value === field
    ? sortDirection.value === 'asc' ? 'bi-arrow-up' : 'bi-arrow-down'
    : 'bi-arrow-down-up'
}

function formatDate(value?: string) {
  return formatApiDate(value, true)
}

function openNotification(notification: NotificationSummary) {
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

      <div class="card-body table-responsive p-0">
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

        <table
          v-else
          class="table table-hover align-middle mb-0"
        >
          <thead>
            <tr>
              <th
                v-for="column in [
                  { label: t('title'), field: 'notificationType' },
                  { label: t('message'), field: 'createdAt' },
                  { label: t('recipient'), field: 'id' },
                  { label: t('channel'), field: 'notificationType' },
                  { label: t('deliveryStatus'), field: 'status' },
                  { label: t('created'), field: 'createdAt' }
                ]"
                :key="column.label"
              >
                <button
                  type="button"
                  class="table-sort-button"
                  @click.stop="toggleSort(column.field)"
                >
                  {{ column.label }}
                  <i
                    class="bi ms-1"
                    :class="sortIcon(column.field)"
                    aria-hidden="true"
                  />
                </button>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="pending">
              <td
                colspan="6"
                class="text-center py-4"
              >
                <div class="spinner-border spinner-border-sm text-primary" />
              </td>
            </tr>
            <tr v-else-if="!rows.length">
              <td
                colspan="6"
                class="text-center py-4"
              >
                <div class="empty-state">
                  <i class="bi bi-bell" />
                  <p>{{ t('noNotifications') }}</p>
                </div>
              </td>
            </tr>
            <tr
              v-for="n in rows"
              :key="n.id"
              class="table-row-link"
              :class="{ 'is-disabled': !n.repairRequest?.id }"
              :tabindex="n.repairRequest?.id ? 0 : -1"
              @click="openNotification(n)"
              @keydown.enter="openNotification(n)"
            >
              <td class="fw-semibold">
                {{ n.title || '-' }}
              </td>
              <td>
                {{ n.message || '-' }}
              </td>
              <td>
                {{ n.recipient?.name || n.recipient?.type || '-' }}
              </td>
              <td>{{ n.channel || '-' }}</td>
              <td>
                <span
                  class="status-chip"
                  :class="n.deliveryStatus === 'DELIVERED' ? 'status-completed' : n.deliveryStatus === 'FAILED' || n.deliveryStatus === 'SKIPPED' ? 'status-cancelled' : 'status-assigned'"
                >
                  <span class="status-dot" />{{ n.deliveryStatus }}
                </span>
              </td>
              <td class="text-nowrap">
                {{ formatDate(n.createdAt) }}
              </td>
            </tr>
          </tbody>
        </table>
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
