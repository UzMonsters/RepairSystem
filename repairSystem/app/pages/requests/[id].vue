<script setup lang="ts">
import { requestStatuses } from '~/lib/statuses'
import type { RepairRequest, RequestStatus, Technician } from '~/types'

const route = useRoute()
const id = Number(route.params.id)

const { data: request, pending, error, refresh } = await useAsyncData(`request-${id}`, () =>
  apiFetch<RepairRequest>(`/requests/${id}`)
)

const { data: technicians } = await useAsyncData(`request-${id}-technicians`, () =>
  apiFetch<Technician[]>('/technicians')
)

const statusForm = ref<RequestStatus>('NEW')
const assignForm = ref<number | ''>('')
const savingStatus = ref(false)
const savingAssign = ref(false)
const message = ref('')
const errorMessage = computed(() => {
  const err = error.value as { data?: { message?: string } } | null
  return err?.data?.message || error.value?.message || 'Failed to load request.'
})

watchEffect(() => {
  if (request.value) {
    statusForm.value = request.value.status
    assignForm.value = request.value.technicianId ?? ''
  }
})

const categoryName = computed(() => {
  if (!request.value) return '-'
  return typeof request.value.category === 'string'
    ? request.value.category
    : request.value.category?.name ?? '-'
})

const customerName = computed(() => request.value?.customer?.name || '-')

function formatDate(value?: string) {
  return value ? new Date(value).toLocaleString() : '-'
}

async function saveStatus() {
  message.value = ''
  savingStatus.value = true
  try {
    await apiFetch(`/requests/${id}/status`, { method: 'PATCH', body: { status: statusForm.value } })
    message.value = 'Status updated successfully.'
    refresh()
  } catch (e) {
    const err = e as { data?: { message?: string } }
    message.value = err.data?.message || 'Failed to update status.'
  } finally {
    savingStatus.value = false
  }
}

async function saveAssign() {
  message.value = ''
  savingAssign.value = true
  try {
    await apiFetch(`/requests/${id}/assign`, { method: 'PATCH', body: { technicianId: assignForm.value } })
    message.value = 'Technician assigned successfully.'
    refresh()
  } catch (e) {
    const err = e as { data?: { message?: string } }
    message.value = err.data?.message || 'Failed to assign technician.'
  } finally {
    savingAssign.value = false
  }
}
</script>

<template>
  <AppContent
    :title="`Request #${id}`"
    :breadcrumbs="[{ label: 'Home', to: '/' }, { label: 'Requests', to: '/requests' }, { label: `#${id}` }]"
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

      <template v-else-if="request">
        <div
          v-if="message"
          class="alert alert-success py-2"
        >
          {{ message }}
        </div>

        <div class="row">
          <div class="col-lg-8">
            <div class="card mb-4">
              <div class="card-header">
                <h3 class="card-title">
                  Problem Description
                </h3>
              </div>
              <div class="card-body">
                <p class="mb-3">
                  {{ request.description || 'No description provided.' }}
                </p>
                <div
                  v-if="request.photoUrl"
                  class="mt-3"
                >
                  <img
                    :src="request.photoUrl"
                    class="img-fluid rounded border"
                    alt="Request photo"
                  >
                </div>
              </div>
            </div>

            <div class="card mb-4">
              <div class="card-header">
                <h3 class="card-title">
                  Customer Information
                </h3>
              </div>
              <div class="card-body">
                <dl class="row mb-0">
                  <dt class="col-sm-4">
                    Full name
                  </dt>
                  <dd class="col-sm-8">
                    <NuxtLink
                      v-if="request.customerId"
                      :to="`/customers/${request.customerId}`"
                    >{{ customerName }}</NuxtLink>
                    <template v-else>
                      {{ customerName }}
                    </template>
                  </dd>
                  <dt class="col-sm-4">
                    Phone number
                  </dt>
                  <dd class="col-sm-8">
                    {{ request.customer?.phone || '-' }}
                  </dd>
                  <dt class="col-sm-4">
                    Telegram Chat ID
                  </dt>
                  <dd class="col-sm-8">
                    {{ request.customer?.telegramChatId ?? '-' }}
                  </dd>
                  <dt class="col-sm-4">
                    Address
                  </dt>
                  <dd class="col-sm-8">
                    {{ request.address || '-' }}
                  </dd>
                </dl>
              </div>
            </div>
          </div>

          <div class="col-lg-4">
            <div class="card mb-4">
              <div class="card-header">
                <h3 class="card-title">
                  Current Status
                </h3>
              </div>
              <div class="card-body">
                <div class="mb-3">
                  <StatusBadge
                    :status="request.status"
                    class="fs-6"
                  />
                </div>
                <div class="input-group">
                  <select
                    v-model="statusForm"
                    class="form-select"
                  >
                    <option
                      v-for="s in requestStatuses"
                      :key="s.value"
                      :value="s.value"
                    >
                      {{ s.label }}
                    </option>
                  </select>
                  <button
                    type="button"
                    class="btn btn-primary"
                    :disabled="savingStatus"
                    @click="saveStatus"
                  >
                    {{ savingStatus ? 'Saving...' : 'Update' }}
                  </button>
                </div>
              </div>
            </div>

            <div class="card mb-4">
              <div class="card-header">
                <h3 class="card-title">
                  Assigned Technician
                </h3>
              </div>
              <div class="card-body">
                <div class="d-flex align-items-center mb-3">
                  <i class="bi bi-person-wrench fs-3 me-2 text-primary" />
                  <div>
                    <div class="fw-semibold">
                      {{ request.technician?.fullName || 'Not assigned' }}
                    </div>
                    <div class="text-muted small">
                      {{ request.technician?.phone || '' }}
                    </div>
                  </div>
                </div>
                <div class="input-group">
                  <select
                    v-model="assignForm"
                    class="form-select"
                  >
                    <option
                      :value="''"
                      disabled
                    >
                      Select technician...
                    </option>
                    <option
                      v-for="t in technicians"
                      :key="t.id"
                      :value="t.id"
                    >
                      {{ t.fullName }}
                    </option>
                  </select>
                  <button
                    type="button"
                    class="btn btn-primary"
                    :disabled="savingAssign || assignForm === ''"
                    @click="saveAssign"
                  >
                    {{ savingAssign ? 'Saving...' : 'Assign' }}
                  </button>
                </div>
              </div>
            </div>

            <div class="card mb-4">
              <div class="card-header">
                <h3 class="card-title">
                  Details
                </h3>
              </div>
              <div class="card-body">
                <dl class="row mb-0">
                  <dt class="col-5">
                    Category
                  </dt>
                  <dd class="col-7">
                    {{ categoryName }}
                  </dd>
                  <dt class="col-5">
                    Created
                  </dt>
                  <dd class="col-7">
                    {{ formatDate(request.createdAt) }}
                  </dd>
                  <dt class="col-5">
                    Last updated
                  </dt>
                  <dd class="col-7">
                    {{ formatDate(request.updatedAt) }}
                  </dd>
                </dl>
              </div>
            </div>
          </div>
        </div>
      </template>
    </template>
  </AppContent>
</template>
