<script setup lang="ts">
import type { Category, Page, RepairRequest } from '~/types'
import { getApiErrorMessage } from '~/utils/api'
import { formatDate } from '~/utils/date'

const { t } = useLocale()
const route = useRoute()
const id = String(route.params.id)

const { data: category, error: categoryError } = await useAsyncData(`category-${id}`, () => apiFetch<Category>(`/categories/${id}`))
const { data: requests, pending } = await useAsyncData(`category-${id}-requests`, () => apiFetch<Page<RepairRequest>>('/requests', {
  query: { categoryId: id, page: 0, size: 20, sort: 'createdAt,desc' }
}))

const errorMessage = computed(() => getApiErrorMessage(categoryError.value, 'Failed to load category.'))
function requestLabel(request: RepairRequest) {
  return request.description || request.category?.name || t('request')
}
</script>

<template>
  <AppContent
    :title="category?.name || t('categories')"
    :breadcrumbs="[{ label: t('home'), to: '/admin' }, { label: t('categories'), to: '/admin/categories' }, { label: category?.name || id }]"
  >
    <div
      v-if="categoryError"
      class="alert alert-danger"
    >
      {{ errorMessage }}
    </div>
    <div
      v-else
      class="card"
    >
      <div class="card-header d-flex justify-content-between align-items-center">
        <h3 class="card-title mb-0">
          {{ category?.name }}
        </h3>
        <span
          class="badge"
          :class="category?.active ? 'text-bg-success' : 'text-bg-secondary'"
        >{{ t(category?.active ? 'active' : 'inactive') }}</span>
      </div>
      <div class="card-body">
        <p class="text-muted">
          {{ category?.description || '-' }}
        </p>
        <h4 class="h6 mt-4">
          {{ t('relatedRequests') }}
        </h4>
        <div
          v-if="pending"
          class="text-center py-4"
        >
          <div class="spinner-border spinner-border-sm text-primary" />
        </div>
        <div
          v-else-if="!requests?.content.length"
          class="empty-state table-empty-state"
        >
          <i class="bi bi-clipboard" /><p>{{ t('noRequestsFound') }}</p>
        </div>
        <div
          v-else
          class="list-group list-group-flush"
        >
          <NuxtLink
            v-for="request in requests.content"
            :key="request.id"
            :to="`/admin/requests/${request.id}`"
            class="list-group-item list-group-item-action category-request-link d-flex justify-content-between align-items-center"
          >
            <span>{{ requestLabel(request) }}</span>
            <span class="small text-muted">{{ formatDate(request.createdAt, true) }}</span>
          </NuxtLink>
        </div>
      </div>
    </div>
  </AppContent>
</template>
