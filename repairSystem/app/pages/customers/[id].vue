<script setup lang="ts">
import type { Customer, RepairRequest } from '~/types'

const { t } = useLocale()
const route = useRoute()
const id = Number(route.params.id)

const { data: customer, pending, error, refresh } = await useAsyncData(`customer-${id}`, () =>
  apiFetch<Customer>(`/customers/${id}`)
)

const { data: history, error: historyError, refresh: refreshHistory } = await useAsyncData(`customer-${id}-requests`, () =>
  apiFetch<RepairRequest[]>(`/customers/${id}/requests`)
)

const errorMessage = computed(() => {
  const err = error.value as { data?: { message?: string } } | null
  return err?.data?.message || error.value?.message || 'Failed to load customer.'
})

const completedRequests = computed(() => {
  if (customer.value?.completedRequests != null) return customer.value.completedRequests
  return (history.value ?? []).filter(r => r.status === 'COMPLETED').length
})

function categoryName(r: RepairRequest) {
  return typeof r.category === 'string' ? r.category : r.category?.name ?? '-'
}

function formatDate(value?: string) {
  return value ? new Date(value).toLocaleDateString() : '-'
}
</script>

<template>
  <AppContent
    :title="customer?.name || `Customer #${id}`"
    :breadcrumbs="[{ label: t('home'), to: '/' }, { label: t('customers'), to: '/customers' }, { label: customer?.name || `#${id}` }]"
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

    <template v-else>
      <div
        v-if="pending"
        class="text-center py-5"
      >
        <div class="spinner-border text-primary" />
      </div>

      <div
        v-else-if="customer"
        class="row"
      >
        <div class="col-lg-4">
          <div class="card mb-4">
            <div class="card-body text-center">
              <span
                class="d-inline-flex align-items-center justify-content-center rounded-circle bg-primary text-white shadow mb-3"
                style="width: 96px; height: 96px; font-size: 36px; font-weight: 600;"
              >
                {{ (customer.name || '?').split(' ').map(p => p[0]).join('').slice(0, 2).toUpperCase() }}
              </span>
              <h5 class="card-title mb-1">
                {{ customer.name }}
              </h5>
              <div class="text-muted small">
                {{ customer.phone }}
              </div>
              <hr>
              <div class="row text-center">
                <div class="col-6">
                  <div class="h4 mb-0">
                    {{ customer.totalRequests ?? 0 }}
                  </div>
                  <div class="text-muted small">
                    Total Requests
                  </div>
                </div>
                <div class="col-6">
                  <div class="h4 mb-0">
                    {{ completedRequests }}
                  </div>
                  <div class="text-muted small">
                    Completed
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="card mb-4">
            <div class="card-header">
              <h3 class="card-title">
                Profile Details
              </h3>
            </div>
            <div class="card-body">
              <dl class="row mb-0">
                <dt class="col-sm-5">
                  Full name
                </dt>
                <dd class="col-sm-7">
                  {{ customer.name }}
                </dd>
                <dt class="col-sm-5">
                  Phone number
                </dt>
                <dd class="col-sm-7">
                  {{ customer.phone }}
                </dd>
                <dt class="col-sm-5">
                  Telegram Chat ID
                </dt>
                <dd class="col-sm-7">
                  {{ customer.telegramChatId ?? '-' }}
                </dd>
                <dt class="col-sm-5">
                  Preferred language
                </dt>
                <dd class="col-sm-7">
                  {{ customer.language || '-' }}
                </dd>
              </dl>
            </div>
          </div>
        </div>

        <div class="col-lg-8">
          <div class="card">
            <div class="card-header">
              <h3 class="card-title">
                Repair History
              </h3>
            </div>
            <div class="card-body table-responsive p-0">
              <div
                v-if="historyError"
                class="alert alert-danger m-3"
              >
                Failed to load repair history.
                <button
                  type="button"
                  class="btn btn-sm btn-outline-danger ms-2"
                  @click="() => refreshHistory()"
                >
                  Retry
                </button>
              </div>
              <table
                v-else
                class="table table-striped align-middle mb-0"
              >
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Category</th>
                    <th>Status</th>
                    <th>Date</th>
                    <th class="text-end">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="!history?.length">
                    <td
                      colspan="5"
                      class="text-center text-muted py-4"
                    >
                      No repair requests yet.
                    </td>
                  </tr>
                  <tr
                    v-for="r in history"
                    :key="r.id"
                  >
                    <td>
                      <NuxtLink :to="`/requests/${r.id}`">
                        #{{ r.id }}
                      </NuxtLink>
                    </td>
                    <td>{{ categoryName(r) }}</td>
                    <td><StatusBadge :status="r.status" /></td>
                    <td>{{ formatDate(r.createdAt) }}</td>
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
      </div>
    </template>
  </AppContent>
</template>
