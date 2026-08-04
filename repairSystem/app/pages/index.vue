<script setup lang="ts">
import type { DashboardData, RepairRequest } from '~/types'

const { data, pending, error, refresh } = await useAsyncData('dashboard', () =>
  apiFetch<DashboardData>('/dashboard/overview')
)

const stats = computed(() => [
  { label: 'Total Requests', value: data.value?.totalRequests ?? 0, icon: 'bi-clipboard-check', color: 'text-bg-primary' },
  { label: 'New Requests', value: data.value?.newRequests ?? 0, icon: 'bi-plus-circle', color: 'text-bg-info' },
  { label: 'Requests In Progress', value: data.value?.inProgress ?? 0, icon: 'bi-gear', color: 'text-bg-warning' },
  { label: 'Completed Requests', value: data.value?.completed ?? 0, icon: 'bi-check-circle', color: 'text-bg-success' },
  { label: 'Total Customers', value: data.value?.totalCustomers ?? 0, icon: 'bi-people', color: 'text-bg-teal' },
  { label: 'Total Technicians', value: data.value?.totalTechnicians ?? 0, icon: 'bi-person-wrench', color: 'text-bg-danger' }
])

const errorMessage = computed(() => {
  const err = error.value as { data?: { message?: string } } | null
  return err?.data?.message || error.value?.message || 'Failed to load dashboard.'
})

function categoryName(r: RepairRequest) {
  return typeof r.category === 'string' ? r.category : r.category?.name ?? '-'
}

function formatTime(value?: string) {
  return value ? new Date(value).toLocaleString() : '-'
}

function activityIcon(type: string) {
  return type === 'NEW_REQUEST' ? 'bi-clipboard-plus' : 'bi-arrow-repeat'
}
</script>

<template>
  <AppContent
    title="Dashboard"
    :breadcrumbs="[{ label: 'Home', href: '#' }, { label: 'Dashboard' }]"
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
        Retry
      </button>
    </div>

    <div
      v-else-if="pending"
      class="text-center py-5"
    >
      <div class="spinner-border text-primary" />
    </div>

    <template v-else>
      <div class="row g-3 g-lg-4">
        <div
          v-for="s in stats"
          :key="s.label"
          class="col-xl-2 col-lg-4 col-md-6"
        >
          <div
            class="small-box"
            :class="s.color"
          >
            <div class="inner">
              <h3>{{ s.value }}</h3>
              <p>{{ s.label }}</p>
            </div>
            <i
              class="small-box-icon bi"
              :class="s.icon"
            />
          </div>
        </div>
      </div>

      <div class="row mt-2">
        <div class="col-lg-7">
          <div class="card mb-4">
            <div class="card-header">
              <h3 class="card-title">
                Recent Requests
              </h3>
              <div class="card-tools">
                <NuxtLink
                  to="/requests"
                  class="btn btn-sm btn-primary"
                >
                  View All
                </NuxtLink>
              </div>
            </div>
            <div class="card-body table-responsive p-0">
              <table class="table table-striped table-hover align-middle mb-0">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Customer</th>
                    <th>Category</th>
                    <th>Status</th>
                    <th>Created</th>
                    <th class="text-end">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="r in data?.recentRequests"
                    :key="r.id"
                  >
                    <td>
                      <NuxtLink :to="`/requests/${r.id}`">
                        #{{ r.id }}
                      </NuxtLink>
                    </td>
                    <td>{{ r.customer?.name || '-' }}</td>
                    <td>{{ categoryName(r) }}</td>
                    <td><StatusBadge :status="r.status" /></td>
                    <td class="text-nowrap">
                      {{ formatTime(r.createdAt) }}
                    </td>
                    <td class="text-end">
                      <NuxtLink
                        :to="`/requests/${r.id}`"
                        class="btn btn-sm btn-outline-secondary"
                        title="View request"
                      >
                        <i class="bi bi-eye" />
                      </NuxtLink>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <div class="col-lg-5">
          <div class="card mb-4">
            <div class="card-header">
              <h3 class="card-title">
                Today's Activity
              </h3>
              <div class="card-tools">
                <span
                  class="badge text-bg-primary"
                >{{ data?.activity.length }}</span>
              </div>
            </div>
            <div class="card-body p-0">
              <ul class="list-group list-group-flush mb-0">
                <li
                  v-for="a in data?.activity"
                  :key="a.id"
                  class="list-group-item d-flex align-items-start gap-3 py-3"
                >
                  <span class="badge text-bg-primary rounded-circle p-2 mt-1">
                    <i
                      class="bi"
                      :class="activityIcon(a.type)"
                    />
                  </span>
                  <div class="flex-grow-1">
                    <div class="fw-semibold small">
                      {{ a.text }}
                    </div>
                    <div class="text-muted small">
                      <i class="bi bi-clock me-1" />{{ formatTime(a.time) }}
                    </div>
                  </div>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </template>
  </AppContent>
</template>
