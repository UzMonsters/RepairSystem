<script setup lang="ts">
import type { Page, Review } from '~/types'
import { getApiErrorMessage } from '~/utils/api'
import { formatDate as formatApiDate } from '~/utils/date'

const { t } = useLocale()
const page = ref(1)
const size = ref(10)
const rating = ref('')

const query = computed(() => ({
  page: page.value - 1,
  size: size.value,
  rating: rating.value || undefined
}))

const { data, pending, error, refresh } = await useAsyncData('reviews-list', () =>
  apiFetch<Page<Review>>('/reviews', { query: query.value })
)

const errorMessage = computed(() => {
  return getApiErrorMessage(error.value, 'Failed to load reviews.')
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
  return formatApiDate(value)
}

function openReview(r: Review) {
  if (r.repairRequestId) navigateTo(`/admin/requests/${r.repairRequestId}`)
}
</script>

<template>
  <AppContent
    :title="t('reviews')"
    :breadcrumbs="[{ label: t('home'), to: '/admin' }, { label: t('reviews') }]"
  >
    <div class="card">
      <div class="card-header">
        <div class="d-flex flex-column flex-md-row gap-2 align-items-md-center justify-content-between">
          <h3 class="card-title mb-0">
            {{ t('customerFeedback') }}
          </h3>
          <select
            v-model="rating"
            class="form-select form-select-sm"
            style="max-width: 140px;"
            @change="applyFilters"
          >
            <option value="">
              {{ t('all') }}
            </option>
            <option value="5">
              5 ★
            </option>
            <option value="4">
              4 ★
            </option>
            <option value="3">
              3 ★
            </option>
            <option value="2">
              2 ★
            </option>
            <option value="1">
              1 ★
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
              <th>{{ t('client') }}</th>
              <th>{{ t('rating') }}</th>
              <th>{{ t('comment') }}</th>
              <th>{{ t('source') }}</th>
              <th>{{ t('date') }}</th>
              <th class="text-end">
                {{ t('actions') }}
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
                  <i class="bi bi-star" />
                  <p>{{ t('noReviewsFound') }}</p>
                </div>
              </td>
            </tr>
            <tr
              v-for="r in rows"
              :key="r.reviewId ?? `${r.repairRequestId}-${r.rating}-${r.customerName}`"
              class="table-row-link"
              :class="{ 'is-disabled': !r.repairRequestId }"
              tabindex="0"
              @click="openReview(r)"
              @keydown.enter="openReview(r)"
            >
              <td class="fw-semibold">
                {{ r.customerName || '-' }}
              </td>
              <td>
                <span class="star-rating">
                  <i
                    v-for="s in 5"
                    :key="s"
                    class="bi"
                    :class="s <= r.rating ? 'bi-star-fill filled' : 'bi-star'"
                  />

                </span>
              </td>
              <td>{{ r.comment || '-' }}</td>
              <td>
                <span
                  v-if="r.source"
                  class="status-chip status-assigned"
                >
                  <span class="status-dot" />{{ r.source }}
                </span>
                <span v-else>-</span>
              </td>
              <td class="text-nowrap">
                {{ formatDate(r.submittedAt) }}
              </td>
              <td class="text-end">
                <button
                  v-if="r.repairRequestId"
                  type="button"
                  class="btn btn-sm btn-outline-secondary"
                  :title="t('view')"
                  @click.stop="openReview(r)"
                >
                  <i class="bi bi-eye" />
                </button>
                <span v-else>-</span>
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
