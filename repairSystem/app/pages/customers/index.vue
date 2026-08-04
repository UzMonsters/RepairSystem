<script setup lang="ts">
import type { Customer } from '~/types'

const { t } = useLocale()
const search = ref('')

const { data, pending, error, refresh } = await useAsyncData('customers-list', () =>
  apiFetch<Customer[]>('/customers')
)

const errorMessage = computed(() => {
  const err = error.value as { data?: { message?: string } } | null
  return err?.data?.message || error.value?.message || 'Failed to load customers.'
})

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return data.value ?? []
  return (data.value ?? []).filter(c =>
    c.name.toLowerCase().includes(q) || c.phone.toLowerCase().includes(q)
  )
})
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
            Customer List
          </h3>
          <div
            class="input-group input-group-sm"
            style="max-width: 260px;"
          >
            <input
              v-model="search"
              type="search"
              class="form-control"
              placeholder="Search by name or phone..."
            >
            <button
              type="button"
              class="btn btn-outline-secondary"
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
            Retry
          </button>
        </div>

        <table
          v-else
          class="table table-striped table-hover align-middle mb-0"
        >
          <thead>
            <tr>
              <th>Name</th>
              <th>Phone</th>
              <th>Total Requests</th>
              <th class="text-end">
                Actions
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="pending">
              <td
                colspan="4"
                class="text-center py-4"
              >
                <div class="spinner-border spinner-border-sm text-primary" />
              </td>
            </tr>
            <tr v-else-if="!filtered.length">
              <td
                colspan="4"
                class="text-center text-muted py-4"
              >
                No customers found.
              </td>
            </tr>
            <tr
              v-for="c in filtered"
              :key="c.id"
            >
              <td>
                <NuxtLink :to="`/customers/${c.id}`">
                  {{ c.name }}
                </NuxtLink>
              </td>
              <td>{{ c.phone }}</td>
              <td>{{ c.totalRequests ?? 0 }}</td>
              <td class="text-end">
                <NuxtLink
                  :to="`/customers/${c.id}`"
                  class="btn btn-sm btn-outline-secondary"
                  title="View profile"
                >
                  <i class="bi bi-person" />
                </NuxtLink>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </AppContent>
</template>
