<script setup lang="ts">
import type { Customer, Page, RepairRequest } from '~/types'
import { getApiErrorMessage } from '~/utils/api'
import { formatDate as formatApiDate } from '~/utils/date'

const { t } = useLocale()
const route = useRoute()
const id = Number(route.params.id)

const { data: customer, pending, error, refresh } = await useAsyncData(`customer-${id}`, () =>
  apiFetch<Customer>(`/customers/${id}`)
)

const { data: history, error: historyError, refresh: refreshHistory } = await useAsyncData(`customer-${id}-requests`, () =>
  apiFetch<Page<RepairRequest>>(`/customers/${id}/requests`, { query: { page: 0, size: 20 } })
)

const errorMessage = computed(() => {
  return getApiErrorMessage(error.value, 'Failed to load customer.')
})

const historyRows = computed(() => history.value?.content ?? [])
const totalRequests = computed(() => history.value?.totalElements ?? 0)
const completedRequests = computed(() => historyRows.value.filter(r => r.status === 'COMPLETED').length)

const initials = computed(() => (customer.value?.fullName || '?').split(' ').map(p => p[0]).join('').slice(0, 2).toUpperCase())

function categoryName(r: RepairRequest) {
  return r.category?.nameRu || r.category?.nameEn || '-'
}

function formatDate(value?: string) {
  return formatApiDate(value)
}
</script>

<template>
  <AppContent
    :title="customer?.fullName || `Customer #${id}`"
    :breadcrumbs="[{ label: t('home'), to: '/' }, { label: t('customers'), to: '/customers' }, { label: customer?.fullName || `#${id}` }]"
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
                {{ initials }}
              </span>
              <h5 class="card-title mb-1">
                {{ customer.fullName }}
              </h5>
              <div class="text-muted small">
                {{ customer.phone }}
              </div>
              <hr>
              <div class="row text-center">
                <div class="col-6">
                  <div class="h4 mb-0">
                    {{ totalRequests }}
                  </div>
                  <div class="text-muted small">
                    {{ t('totalRequests') }}
                  </div>
                </div>
                <div class="col-6">
                  <div class="h4 mb-0">
                    {{ completedRequests }}
                  </div>
                  <div class="text-muted small">
                    {{ t('completedTotal') }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="card mb-4">
            <div class="card-header">
              <h3 class="card-title">
                {{ t('profileDetails') }}
              </h3>
            </div>
            <div class="card-body">
              <dl class="row mb-0">
                <dt class="col-sm-5">
                  {{ t('fullName') }}
                </dt>
                <dd class="col-sm-7">
                  {{ customer.fullName }}
                </dd>
                <dt class="col-sm-5">
                  {{ t('phone') }}
                </dt>
                <dd class="col-sm-7">
                  {{ customer.phone }}
                </dd>
                <dt class="col-sm-5">
                  {{ t('preferredLanguage') }}
                </dt>
                <dd class="col-sm-7">
                  {{ customer.preferredLanguage || '-' }}
                </dd>
                <dt class="col-sm-5">
                  {{ t('registrationSource') }}
                </dt>
                <dd class="col-sm-7">
                  {{ customer.registrationSource || '-' }}
                </dd>
                <dt class="col-sm-5">
                  {{ t('telegramLinked') }}
                </dt>
                <dd class="col-sm-7">
                  <span
                    class="badge"
                    :class="customer.telegramLinked ? 'text-bg-success' : 'text-bg-secondary'"
                  >
                    {{ customer.telegramLinked ? t('yes') : t('no') }}
                  </span>
                </dd>
                <dt class="col-sm-5">
                  {{ t('active') }}
                </dt>
                <dd class="col-sm-7">
                  <span
                    class="badge"
                    :class="customer.active ? 'text-bg-success' : 'text-bg-secondary'"
                  >
                    {{ customer.active ? t('active') : t('inactive') }}
                  </span>
                </dd>
                <dt class="col-sm-5">
                  {{ t('created') }}
                </dt>
                <dd class="col-sm-7">
                  {{ formatDate(customer.createdAt) }}
                </dd>
              </dl>
            </div>
          </div>
        </div>

        <div class="col-lg-8">
          <div class="card">
            <div class="card-header">
              <h3 class="card-title">
                {{ t('repairHistory') }}
              </h3>
            </div>
            <div class="card-body table-responsive p-0">
              <div
                v-if="historyError"
                class="alert alert-danger m-3"
              >
                {{ t('failedToLoadHistory') }}
                <button
                  type="button"
                  class="btn btn-sm btn-outline-danger ms-2"
                  @click="() => refreshHistory()"
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
                    <th>#</th>
                    <th>{{ t('categories') }}</th>
                    <th>{{ t('status') }}</th>
                    <th>{{ t('date') }}</th>
                    <th class="text-end">
                      {{ t('actions') }}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="!historyRows.length">
                    <td
                      colspan="5"
                      class="text-center py-4"
                    >
                      <div class="empty-state">
                        <i class="bi bi-clipboard" />
                        <p>{{ t('noRequestsFound') }}</p>
                      </div>
                    </td>
                  </tr>
                  <tr
                    v-for="r in historyRows"
                    :key="r.id"
                  >
                    <td>
                      <NuxtLink :to="`/requests/${r.id}`">
                        {{ r.requestNumber || `#${r.id}` }}
                      </NuxtLink>
                    </td>
                    <td>{{ categoryName(r) }}</td>
                    <td><StatusBadge :status="r.status" /></td>
                    <td>{{ formatDate(r.createdAt) }}</td>
                    <td class="text-end">
                      <NuxtLink
                        :to="`/requests/${r.id}`"
                        class="btn btn-sm btn-outline-secondary"
                        :title="t('view')"
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
