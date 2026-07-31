<script setup lang="ts">
import type { Review } from '~/types'

const { data, pending, error, refresh } = await useAsyncData('reviews-list', () =>
  apiFetch<Review[]>('/reviews')
)

const errorMessage = computed(() => {
  const err = error.value as { data?: { message?: string } } | null
  return err?.data?.message || error.value?.message || 'Failed to load reviews.'
})

const customerName = (r: Review) => r.customer || r.customerName || '-'

function formatDate(value?: string) {
  return value ? new Date(value).toLocaleDateString() : '-'
}
</script>

<template>
  <AppContent
    title="Reviews"
    :breadcrumbs="[{ label: 'Home', to: '/' }, { label: 'Reviews' }]"
  >
    <div class="card">
      <div class="card-header">
        <h3 class="card-title">
          Customer Feedback
        </h3>
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
              <th>Customer</th>
              <th>Rating</th>
              <th>Comment</th>
              <th>Related Request</th>
              <th>Date</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="pending">
              <td
                colspan="5"
                class="text-center py-4"
              >
                <div class="spinner-border spinner-border-sm text-primary" />
              </td>
            </tr>
            <tr v-else-if="!data?.length">
              <td
                colspan="5"
                class="text-center text-muted py-4"
              >
                No reviews yet.
              </td>
            </tr>
            <tr
              v-for="r in data"
              :key="r.id ?? `${r.requestId}-${r.rating}-${r.customer}`"
            >
              <td class="fw-semibold">
                {{ customerName(r) }}
              </td>
              <td><RatingStars :rating="r.rating" /></td>
              <td>{{ r.comment || '-' }}</td>
              <td>
                <NuxtLink
                  v-if="r.requestId"
                  :to="`/requests/${r.requestId}`"
                >
                  #{{ r.requestId }}
                </NuxtLink>
                <span v-else>-</span>
              </td>
              <td>{{ formatDate(r.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </AppContent>
</template>
