<script setup lang="ts">
import type { Technician } from '~/types'
import { getApiErrorMessage } from '~/utils/api'
import { formatDate as formatApiDate } from '~/utils/date'

const { t } = useLocale()
const route = useRoute()
const id = Number(route.params.id)

const { data: technician, pending, error, refresh } = await useAsyncData(`technician-${id}`, () =>
  apiFetch<Technician & { notes?: string }>(`/technicians/${id}`)
)

const { data: workload, error: workloadError, refresh: refreshWorkload } = await useAsyncData(`technician-${id}-workload`, () =>
  apiFetch<Record<string, unknown>>(`/technicians/${id}/workload`)
)

const errorMessage = computed(() => getApiErrorMessage(error.value, 'Failed to load technician.'))
const workloadErrorMessage = computed(() => getApiErrorMessage(workloadError.value, 'Failed to load technician workload.'))

function formatDate(value?: string) {
  return formatApiDate(value)
}
</script>

<template>
  <AppContent
    :title="technician?.fullName || `Technician #${id}`"
    :breadcrumbs="[{ label: t('home'), to: '/admin' }, { label: t('technicians'), to: '/admin/technicians' }, { label: technician?.fullName || `#${id}` }]"
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

    <div
      v-else-if="technician"
      class="row g-4"
    >
      <div class="col-lg-5">
        <div class="card h-100">
          <div class="card-header d-flex justify-content-between align-items-center">
            <h3 class="card-title mb-0">
              {{ t('profileDetails') }}
            </h3>
            <span
              class="badge"
              :class="technician.active ? 'text-bg-success' : 'text-bg-secondary'"
            >
              {{ technician.active ? t('active') : t('inactive') }}
            </span>
          </div>
          <div class="card-body">
            <dl class="row mb-0">
              <dt class="col-sm-6">
                {{ t('fullName') }}
              </dt>
              <dd class="col-sm-6">
                {{ technician.fullName }}
              </dd>
              <dt class="col-sm-6">
                {{ t('phone') }}
              </dt>
              <dd class="col-sm-6">
                {{ technician.phone || '-' }}
              </dd>
              <dt class="col-sm-6">
                {{ t('specialization') }}
              </dt>
              <dd class="col-sm-6">
                {{ technician.specialization || '-' }}
              </dd>
              <dt class="col-sm-6">
                {{ t('maxConcurrentRequests') }}
              </dt>
              <dd class="col-sm-6">
                {{ technician.maximumConcurrentRequests ?? '-' }}
              </dd>
              <dt class="col-sm-6">
                {{ t('preferredLanguage') }}
              </dt>
              <dd class="col-sm-6">
                {{ technician.preferredLanguage ? t(`language.${technician.preferredLanguage}`) : '-' }}
              </dd>
              <dt class="col-sm-6">
                {{ t('telegramLinked') }}
              </dt>
              <dd class="col-sm-6">
                {{ technician.telegramLinked ? t('yes') : t('no') }}
              </dd>
              <dt class="col-sm-6">
                {{ t('created') }}
              </dt>
              <dd class="col-sm-6">
                {{ formatDate(technician.createdAt) }}
              </dd>
            </dl>
            <div
              v-if="technician.notes"
              class="border-top pt-3 mt-3"
            >
              <div class="text-muted small mb-1">
                {{ t('description') }}
              </div>
              <p class="mb-0">
                {{ technician.notes }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <div class="col-lg-7">
        <div class="card h-100">
          <div class="card-header d-flex justify-content-between align-items-center">
            <h3 class="card-title mb-0">
              {{ t('workload') }}
            </h3>
            <button
              type="button"
              class="btn btn-sm btn-outline-secondary"
              :title="t('retry')"
              @click="() => refreshWorkload()"
            >
              <i class="bi bi-arrow-clockwise" />
            </button>
          </div>
          <div class="card-body">
            <div
              v-if="workloadError"
              class="alert alert-danger"
            >
              {{ workloadErrorMessage }}
            </div>
            <div
              v-else-if="workload"
              class="workload-grid"
            >
              <div class="workload-item">
                <span class="workload-label">{{ t('active') }}</span>
                <span class="workload-value">{{ workload.active ? t('yes') : t('no') }}</span>
              </div>
              <div class="workload-item">
                <span class="workload-label">{{ t('maxConcurrentRequests') }}</span>
                <span class="workload-value">{{ workload.maximumConcurrentRequests }}</span>
              </div>
              <div class="workload-item">
                <span class="workload-label">{{ t('status.NEW') }}</span>
                <span class="workload-value">{{ workload.pendingAssignments }}</span>
              </div>
              <div class="workload-item">
                <span class="workload-label">{{ t('status.IN_PROGRESS') }}</span>
                <span class="workload-value">{{ workload.acceptedAssignments }}</span>
              </div>
              <div class="workload-item">
                <span class="workload-label">{{ t('totalRequests') }}</span>
                <span class="workload-value">{{ workload.totalActiveAssignments }}</span>
              </div>
              <div class="workload-item">
                <span class="workload-label">{{ t('remainingCapacity') }}</span>
                <span class="workload-value">{{ workload.remainingCapacity }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </AppContent>
</template>
