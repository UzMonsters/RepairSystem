<script setup lang="ts">
import type { NotificationSummary, Page } from '~/types'

const { t } = useLocale()
const page = ref(1)
const size = ref(10)
const status = ref('')

const query = computed(() => ({
  page: page.value - 1,
  size: size.value,
  status: status.value || undefined,
  sort: 'createdAt,desc'
}))

const { data, pending, error, refresh } = await useAsyncData('notifications', () =>
  apiFetch<Page<NotificationSummary>>('/notifications', { query: query.value })
)

const errorMessage = computed(() => {
  const err = error.value as { data?: { message?: string } } | null
  return err?.data?.message || error.value?.message || 'Failed to load notifications.'
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

function formatDate(value?: string) {
  return value ? new Date(value).toLocaleString() : '-'
}

const statuses = ['PENDING', 'PROCESSING', 'RETRY_SCHEDULED', 'DELIVERED', 'SKIPPED', 'DEAD']
</script>

<template>
  <AppContent
    :title="t('notifications')"
    :breadcrumbs="[{ label: t('home'), to: '/' }, { label: t('notifications') }]"
  >
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
            <option value="">{{ t('all') }}</option>
            <option v-for="s in statuses" :key="s" :value="s">{{ s }}</option>
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
              <th>{{ t('eventKey') }}</th>
              <th>{{ t('type') }}</th>
              <th>{{ t('recipient') }}</th>
              <th>{{ t('relatedRequest') }}</th>
              <th>{{ t('status') }}</th>
              <th>{{ t('attempts') }}</th>
              <th>{{ t('created') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="pending">
              <td
                colspan="7"
                class="text-center py-4"
              >
                <div class="spinner-border spinner-border-sm text-primary" />
              </td>
            </tr>
            <tr v-else-if="!rows.length">
              <td
                colspan="7"
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
            >
              <td class="fw-semibold">
                {{ n.eventKey || n.notificationType || '-' }}
              </td>
              <td>{{ n.notificationType || '-' }}</td>
              <td>{{ n.recipientType || '-' }}<template v-if="n.recipientId"> · {{ n.recipientId }}</template></td>
              <td>
                <NuxtLink
                  v-if="n.repairRequestId"
                  :to="`/requests/${n.repairRequestId}`"
                >
                  {{ n.requestNumber || `#${n.repairRequestId}` }}
                </NuxtLink>
                <span v-else>-</span>
              </td>
              <td>
                <span
                  class="status-chip"
                  :class="n.status === 'DELIVERED' ? 'status-completed' : n.status === 'DEAD' ? 'status-cancelled' : n.status === 'SKIPPED' ? 'status-cancelled' : 'status-assigned'"
                >
                  <span class="status-dot" />{{ n.status }}
                </span>
              </td>
              <td>{{ n.attemptCount }}</td>
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
