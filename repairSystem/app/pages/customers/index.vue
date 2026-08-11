<script setup lang="ts">
import type { Customer, Page } from '~/types'
import { getApiErrorMessage } from '~/utils/api'

const { t } = useLocale()
const search = ref('')
const page = ref(1)
const size = ref(10)

const query = computed(() => ({
  page: page.value - 1,
  size: size.value,
  search: search.value.trim() || undefined
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

function formatDate(value?: string) {
  return value ? new Date(value).toLocaleDateString() : '-'
}
</script>

<template>
  <AppContent
    :title="t('customers')"
    :breadcrumbs="[{ label: t('home'), to: '/' }, { label: t('customers') }]"
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
              <th>{{ t('name') }}</th>
              <th>{{ t('phone') }}</th>
              <th>{{ t('language') }}</th>
              <th>{{ t('telegramLinked') }}</th>
              <th>{{ t('active') }}</th>
              <th>{{ t('created') }}</th>
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
            >
              <td>
                <NuxtLink
                  :to="`/customers/${c.id}`"
                  class="fw-semibold link-primary"
                >
                  {{ c.fullName }}
                </NuxtLink>
              </td>
              <td>{{ c.phone }}</td>
              <td>{{ c.preferredLanguage ? t(`language.${c.preferredLanguage}`) : '-' }}</td>
              <td>
                <span
                  v-if="c.telegramLinked"
                  class="status-chip status-completed"
                >
                  <span class="status-dot" />TG
                </span>
                <span v-else>-</span>
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
                  :to="`/customers/${c.id}`"
                  class="btn btn-sm btn-outline-secondary"
                  :title="t('view')"
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
