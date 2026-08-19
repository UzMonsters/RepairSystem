<script setup lang="ts">
import type { DashboardOverview, Page, RepairRequest } from '~/types'
import { getApiErrorMessage } from '~/utils/api'
import { formatDate } from '~/utils/date'

const { t } = useLocale()
const { data, pending, error, refresh } = useAsyncData('dashboard', () =>
  apiFetch<DashboardOverview>('/dashboard/overview')
)

const { data: recentRequests } = useAsyncData('dashboard-recent', () =>
  apiFetch<Page<RepairRequest>>('/requests', { query: { page: 0, size: 6, sort: 'createdAt,desc' } })
)

const completionRate = computed(() => {
  const total = data.value?.totalRequests ?? 0
  if (!total) return null
  return ((data.value!.completedTotal / total) * 100).toFixed(1)
})

const cancellationRate = computed(() => {
  const total = data.value?.totalRequests ?? 0
  if (!total) return null
  return ((data.value!.cancelledTotal / total) * 100).toFixed(1)
})

const averageRating = computed(() => {
  const r = data.value?.averageRating
  return r == null ? '-' : Number(r).toFixed(1)
})

const stats = computed(() => [
  { icon: 'bi-clipboard-check', title: t('totalRequests'), value: data.value?.totalRequests ?? 0, sub: t('all') },
  { icon: 'bi-plus-circle', title: t('newToday'), value: data.value?.newToday ?? 0, sub: data.value?.businessDate ? formatDate(data.value.businessDate) : '-' },
  { icon: 'bi-folder2-open', title: t('openRequests'), value: data.value?.openRequests ?? 0, sub: t('all') },
  { icon: 'bi-gear', title: t('inProgress'), value: data.value?.inProgress ?? 0, sub: t('all') },
  { icon: 'bi-hourglass-split', title: t('waitingForParts'), value: data.value?.waitingForParts ?? 0, sub: t('all') },
  { icon: 'bi-check-circle', title: t('completedTotal'), value: data.value?.completedTotal ?? 0, rate: completionRate.value ? `${completionRate.value}%` : undefined, isUp: true },
  { icon: 'bi-calendar-check', title: t('completedToday'), value: data.value?.completedToday ?? 0, sub: t('today') },
  { icon: 'bi-x-circle', title: t('cancelledTotal'), value: data.value?.cancelledTotal ?? 0, rate: cancellationRate.value ? `${cancellationRate.value}%` : undefined, isUp: false },
  { icon: 'bi-star', title: t('averageRating'), value: averageRating.value, sub: `${data.value?.totalReviews ?? 0} ${t('reviews')}` },
  { icon: 'bi-chat-quote', title: t('totalReviews'), value: data.value?.totalReviews ?? 0, sub: t('all') },
  { icon: 'bi-person-wrench', title: t('activeTechnicians'), value: data.value?.activeTechnicians ?? 0, sub: t('all') },
  { icon: 'bi-person-check', title: t('techniciansWithActiveWork'), value: data.value?.techniciansWithActiveWork ?? 0, sub: t('all') }
])

const statusSummary = computed(() => [
  { status: 'NEW', label: t('status.NEW'), value: data.value?.newToday ?? 0, badge: 'status-new' },
  { status: 'IN_PROGRESS', label: t('status.IN_PROGRESS'), value: data.value?.inProgress ?? 0, badge: 'status-in-progress' },
  { status: 'WAITING_FOR_PARTS', label: t('status.WAITING_FOR_PARTS'), value: data.value?.waitingForParts ?? 0, badge: 'status-waiting' },
  { status: 'COMPLETED', label: t('status.COMPLETED'), value: data.value?.completedTotal ?? 0, badge: 'status-completed' },
  { status: 'CANCELLED', label: t('status.CANCELLED'), value: data.value?.cancelledTotal ?? 0, badge: 'status-cancelled' }
])

const errorMessage = computed(() => {
  return getApiErrorMessage(error.value, 'Failed to load dashboard.')
})

function categoryName(c?: RepairRequest['category']) {
  if (!c) return '-'
  return c.name || '-'
}

function formatTime(value?: string) {
  return formatDate(value, true)
}

function openStatus(status: string) {
  navigateTo({ path: '/admin/requests', query: { status } })
}
</script>

<template>
  <AppContent
    :title="t('dashboard')"
    :breadcrumbs="[{ label: t('home'), to: '/admin' }, { label: t('dashboard') }]"
  >
    <div
      v-if="error"
      class="alert alert-danger"
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
      class="text-center py-5"
    >
      <div class="spinner-border text-primary" />
    </div>

    <template v-else>
      <div class="kpi-grid">
        <KpiCard
          v-for="s in stats"
          :key="s.title"
          :icon="s.icon"
          :title="s.title"
          :value="s.value"
          :rate="s.rate"
          :is-up="s.isUp"
          :sub="s.sub"
        />
      </div>

      <div class="row mt-4 g-4">
        <div class="col-lg-8">
          <div class="card dash-card">
            <div class="card-header d-flex align-items-center justify-content-between">
              <h3 class="card-title mb-0">
                {{ t('recentRequests') }}
              </h3>
              <NuxtLink
                to="/admin/requests"
                class="btn btn-sm btn-primary"
              >
                {{ t('viewAll') }}
              </NuxtLink>
            </div>
            <div class="card-body table-responsive p-0">
              <table class="table table-hover align-middle mb-0">
                <thead>
                  <tr>
                    <th>{{ t('description') }}</th>
                    <th>{{ t('client') }}</th>
                    <th>{{ t('categories') }}</th>
                    <th>{{ t('status') }}</th>
                    <th>{{ t('created') }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="!recentRequests?.content.length">
                    <td
                      colspan="5"
                      class="text-center py-5"
                    >
                      <div class="empty-state">
                        <i class="bi bi-clipboard" />
                        <p>{{ t('noRequestsFound') }}</p>
                      </div>
                    </td>
                  </tr>
                  <tr
                    v-for="r in recentRequests?.content"
                    :key="r.id"
                    class="dashboard-request-row"
                    tabindex="0"
                    @click="navigateTo(`/admin/requests/${r.id}`)"
                    @keydown.enter="navigateTo(`/admin/requests/${r.id}`)"
                  >
                    <td
                      class="dashboard-description"
                      :title="r.description || categoryName(r.category)"
                    >
                      {{ r.description || categoryName(r.category) }}
                    </td>
                    <td>{{ r.customer?.fullName || '-' }}</td>
                    <td>{{ categoryName(r.category) }}</td>
                    <td><StatusBadge :status="r.status" /></td>
                    <td class="text-nowrap">
                      {{ formatTime(r.createdAt) }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <div class="col-lg-4">
          <div class="card dash-card h-100">
            <div class="card-header">
              <h3 class="card-title mb-0">
                {{ t('requests') }} · {{ t('status') }}
              </h3>
            </div>
            <div class="card-body">
              <div
                v-for="s in statusSummary"
                :key="s.label"
                class="status-summary-item"
                role="button"
                tabindex="0"
                @click="openStatus(s.status)"
                @keydown.enter="openStatus(s.status)"
              >
                <span class="status-summary-label">{{ s.label }}</span>
                <span class="status-summary-value">{{ s.value }}</span>
                <div class="status-summary-bar">
                  <div
                    class="status-summary-fill"
                    :class="s.badge"
                    :style="{ width: `${data?.totalRequests ? (s.value / data.totalRequests) * 100 : 0}%` }"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </AppContent>
</template>
