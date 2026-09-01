<script setup lang="ts">
import type { Customer, Page } from '~/types'
import { apiAssetUrl, getApiErrorMessage } from '~/utils/api'
import { formatDate } from '~/utils/date'

const { t } = useLocale()
const search = ref('')
const page = ref(1)
const size = ref(10)
const sortField = ref('createdAt')
const sortDirection = ref<'asc' | 'desc'>('desc')

const query = computed(() => ({
  page: page.value - 1,
  size: size.value,
  search: search.value.trim() || undefined,
  sort: `${sortField.value},${sortDirection.value}`
}))

const { data, pending, error, refresh } = await useAsyncData('customers-list', () =>
  apiFetch<Page<Customer>>('/customers', { query: query.value })
)

const errorMessage = computed(() => {
  return getApiErrorMessage(error.value, 'Failed to load customers.')
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

function openCustomer(id: number) {
  navigateTo(`/admin/customers/${id}`)
}
</script>

<template>
  <AppContent
    :title="t('customers')"
    :breadcrumbs="[{ label: t('home'), to: '/admin' }, { label: t('customers') }]"
  >
    <div class="card">
      <div class="card-header">
        <div class="d-flex flex-column flex-md-row gap-2 align-items-md-center justify-content-between">
          <h3 class="card-title mb-0">
            {{ t('customerList') }}
          </h3>
          <div class="input-group input-group-sm search-box">
            <input
              v-model="search"
              type="search"
              class="form-control"
              :placeholder="t('searchByNameOrPhone')"
              @keyup.enter="applyFilters"
            >
            <button
              type="button"
              class="btn btn-outline-secondary"
              @click="applyFilters"
            >
              <i class="bi bi-search" />
            </button>
          </div>
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
                  { label: t('name'), field: 'fullName' },
                  { label: t('phone'), field: 'phone' },
                  { label: t('language'), field: 'preferredLanguage' },
                  { label: t('binding'), field: 'registrationSource' },
                  { label: t('active'), field: 'active' },
                  { label: t('created'), field: 'createdAt' }
                ]"
                :key="column.field"
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
              <th class="text-end">
                {{ t('actions') }}
              </th>
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
                  <i class="bi bi-people" />
                  <p>{{ t('noCustomersFound') }}</p>
                </div>
              </td>
            </tr>
            <tr
              v-for="c in rows"
              :key="c.id"
              class="table-row-link"
              tabindex="0"
              @click="openCustomer(c.id)"
              @keydown.enter="openCustomer(c.id)"
            >
              <td>
                <img
                  v-if="apiAssetUrl(c.avatar?.downloadUrl)"
                  :src="apiAssetUrl(c.avatar?.downloadUrl)"
                  :alt="c.fullName"
                  class="entity-avatar me-2"
                >
                <span class="fw-semibold">
                  {{ c.fullName }}
                </span>
              </td>
              <td>
                <span v-if="c.phone">{{ c.phone }}</span>
                <span
                  v-else
                  class="text-muted small"
                ><i class="bi bi-telephone-x me-1" />{{ t('phoneNotLinked') }}</span>
              </td>
              <td>{{ c.preferredLanguage ? t(`language.${c.preferredLanguage}`) : '-' }}</td>
              <td>
                <i
                  v-if="c.registrationSource === 'TELEGRAM' || c.telegramLinked"
                  class="bi bi-telegram text-primary fs-5"
                  :title="t('telegramLinked')"
                />
                <i
                  v-else-if="c.registrationSource === 'GOOGLE'"
                  class="bi bi-google text-danger fs-5"
                  title="Google"
                />
                <i
                  v-else-if="c.registrationSource === 'PHONE'"
                  class="bi bi-telephone text-success fs-5"
                  :title="t('phone')"
                />
                <span
                  v-else
                  class="text-muted small"
                ><i class="bi bi-telephone-x me-1" />{{ t('phoneNotLinked') }}</span>
              </td>
              <td>
                <span
                  class="badge"
                  :class="c.active ? 'text-bg-success' : 'text-bg-secondary'"
                >{{ t(c.active ? 'active' : 'inactive') }}</span>
              </td>
              <td class="text-nowrap">
                {{ formatDate(c.createdAt) }}
              </td>
              <td class="text-end text-nowrap">
                <NuxtLink
                  :to="`/admin/customers/${c.id}`"
                  class="btn btn-sm btn-outline-secondary"
                  :title="t('view')"
                  @click.stop
                >
                  <i class="bi bi-person" />
                </NuxtLink>
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
        :page-sizes="[10, 25, 50, 100]"
        @update:page="goToPage"
        @update:size="changeSize"
      />
    </div>
  </AppContent>
</template>
