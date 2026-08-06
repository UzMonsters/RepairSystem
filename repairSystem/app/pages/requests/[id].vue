<script setup lang="ts">
import type { RepairRequest, Technician } from '~/types'

const route = useRoute()
const id = Number(route.params.id)

const { t } = useLocale()

const { data: request, pending, error, refresh } = await useAsyncData(`request-${id}`, () =>
  apiFetch<RepairRequest>(`/requests/${id}`)
)

const { data: technicians } = await useAsyncData(`request-${id}-technicians`, () =>
  apiFetch<{ content: Technician[] }>('/technicians', { query: { size: 100 } })
)

const technicianOptions = computed(() => technicians.value?.content ?? [])

const assignForm = ref<number | ''>('')
const savingAssign = ref(false)
const message = ref('')

const errorMessage = computed(() => {
  const err = error.value as { data?: { message?: string } } | null
  return err?.data?.message || error.value?.message || 'Failed to load request.'
})

const categoryName = computed(() => {
  const c = request.value?.category
  if (!c) return '-'
  return c.nameRu || c.nameEn || '-'
})

const customerName = computed(() => request.value?.customer?.fullName || '-')
const customerPhone = computed(() => request.value?.customer?.phone || '-')
const assignedTechnician = computed(() => request.value?.currentAssignment?.technician ?? null)
const isAssignmentPending = computed(() =>
  request.value?.status === 'ASSIGNED' && request.value?.currentAssignment?.status === 'PENDING'
)

function formatDate(value?: string) {
  return value ? new Date(value).toLocaleString() : '-'
}

const savingAccept = ref(false)

async function acceptAssignment() {
  savingAccept.value = true
  try {
    await apiFetch(`/requests/${id}/assignment/accept`, { method: 'POST' })
    message.value = t('statusUpdated')
    refresh()
  } catch (e) {
    const err = e as { data?: { message?: string }, message?: string }
    message.value = err.data?.message || err.message || 'Failed to accept assignment.'
  } finally {
    savingAccept.value = false
  }
}

async function saveAssign() {
  message.value = ''
  savingAssign.value = true
  try {
    await apiFetch(`/requests/${id}/assign`, { method: 'POST', body: { technicianId: assignForm.value } })
    message.value = t('assignedSuccessfully')
    refresh()
  } catch (e) {
    const err = e as { data?: { message?: string }, message?: string }
    message.value = err.data?.message || err.message || 'Failed to assign technician.'
  } finally {
    savingAssign.value = false
  }
}

const execAction = ref('')
const execForm = ref('')
const savingExec = ref(false)
const execError = ref('')

function openExecModal(action: string) {
  execAction.value = action
  execForm.value = ''
  execError.value = ''
  showModal('exec-modal')
}

async function runExec() {
  savingExec.value = true
  execError.value = ''
  try {
    const body = execAction.value === 'complete'
      ? { workPerformed: execForm.value }
      : execAction.value === 'wait-for-parts' || execAction.value === 'cancel'
        ? { reason: execForm.value }
        : execAction.value === 'resume'
          ? { note: execForm.value || undefined }
          : undefined
    await apiFetch(`/requests/${id}/${execAction.value}`, { method: 'POST', body })
    hideModal('exec-modal')
    message.value = t('statusUpdated')
    refresh()
  } catch (e) {
    const err = e as { data?: { message?: string }, message?: string }
    execError.value = err.data?.message || err.message || 'Action failed.'
  } finally {
    savingExec.value = false
  }
}

function can(action: string) {
  const s = request.value?.status
  if (action === 'start') return s === 'ASSIGNED' && request.value?.currentAssignment?.status === 'ACCEPTED'
  if (action === 'wait-for-parts' || action === 'complete') return s === 'IN_PROGRESS' || s === 'WAITING_FOR_PARTS'
  if (action === 'resume') return s === 'WAITING_FOR_PARTS'
  if (action === 'cancel') return s !== 'COMPLETED' && s !== 'CANCELLED'
  return false
}
</script>

<template>
  <AppContent
    :title="`#${request?.requestNumber || id}`"
    :breadcrumbs="[{ label: t('home'), to: '/' }, { label: t('requests'), to: '/requests' }, { label: `#${request?.requestNumber || id}` }]"
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
                <div class="d-flex align-items-center justify-content-between">
                  <h3 class="card-title mb-0">
                    {{ t('description') }}
                  </h3>
                  <StatusBadge :status="request.status" />
                </div>
              </div>
              <div class="card-body">
                <p class="mb-0">
                  {{ request.description || '-' }}
                </p>
              </div>
            </div>

            <div class="card mb-4">
              <div class="card-header">
                <h3 class="card-title">
                  {{ t('client') }}
                </h3>
              </div>
              <div class="card-body">
                <dl class="row mb-0">
                  <dt class="col-sm-4">
                    {{ t('fullName') }}
                  </dt>
                  <dd class="col-sm-8">
                    <NuxtLink
                      v-if="request.customer?.id"
                      :to="`/customers/${request.customer.id}`"
                    >{{ customerName }}</NuxtLink>
                    <template v-else>
                      {{ customerName }}
                    </template>
                  </dd>
                  <dt class="col-sm-4">
                    {{ t('phone') }}
                  </dt>
                  <dd class="col-sm-8">
                    {{ customerPhone }}
                  </dd>
                  <dt class="col-sm-4">
                    {{ t('address') }}
                  </dt>
                  <dd class="col-sm-8">
                    {{ request.address || '-' }}
                  </dd>
                  <dt class="col-sm-4">
                    {{ t('customerPreferredVisitAt') }}
                  </dt>
                  <dd class="col-sm-8">
                    {{ formatDate(request.customerPreferredVisitAt) }}
                  </dd>
                </dl>
              </div>
            </div>
          </div>

          <div class="col-lg-4">
            <div class="card mb-4">
              <div class="card-header">
                <h3 class="card-title">
                  {{ t('details') }}
                </h3>
              </div>
              <div class="card-body">
                <dl class="row mb-0">
                  <dt class="col-5">
                    {{ t('status') }}
                  </dt>
                  <dd class="col-7">
                    <StatusBadge :status="request.status" />
                  </dd>
                  <dt class="col-5">
                    {{ t('priority') }}
                  </dt>
                  <dd class="col-7">
                    {{ request.priority }}
                  </dd>
                  <dt class="col-5">
                    {{ t('categories') }}
                  </dt>
                  <dd class="col-7">
                    {{ categoryName }}
                  </dd>
                  <dt class="col-5">
                    {{ t('source') }}
                  </dt>
                  <dd class="col-7">
                    {{ request.source }}
                  </dd>
                  <dt class="col-5">
                    {{ t('created') }}
                  </dt>
                  <dd class="col-7">
                    {{ formatDate(request.createdAt) }}
                  </dd>
                  <dt class="col-5">
                    {{ t('lastUpdated') }}
                  </dt>
                  <dd class="col-7">
                    {{ formatDate(request.updatedAt) }}
                  </dd>
                </dl>
              </div>
            </div>

            <div class="card mb-4">
              <div class="card-header">
                <h3 class="card-title">
                  {{ t('assignedTechnician') }}
                </h3>
              </div>
              <div class="card-body">
                <div
                  v-if="assignedTechnician"
                  class="d-flex align-items-center mb-3"
                >
                  <i class="bi bi-person-wrench fs-3 me-2 text-primary" />
                  <div>
                    <div class="fw-semibold">
                      {{ assignedTechnician.fullName }}
                    </div>
                    <div class="text-muted small">
                      {{ assignedTechnician.phone || '' }}
                    </div>
                  </div>
                </div>
                <div
                  v-else
                  class="text-muted mb-3"
                >
                  {{ t('notAssigned') }}
                </div>
                <div
                  v-if="request.status === 'NEW' || request.status === 'ASSIGNED'"
                  class="input-group"
                >
                  <select
                    v-model="assignForm"
                    class="form-select"
                  >
                    <option
                      :value="''"
                      disabled
                    >
                      {{ t('selectTechnician') }}
                    </option>
                    <option
                      v-for="tech in technicianOptions"
                      :key="tech.id"
                      :value="tech.id"
                    >
                      {{ tech.fullName }}
                    </option>
                  </select>
                  <button
                    type="button"
                    class="btn btn-primary"
                    :disabled="savingAssign || assignForm === ''"
                    @click="saveAssign"
                  >
                    {{ savingAssign ? t('saving') : t('assignTechnician') }}
                  </button>
                </div>
                <div
                  v-if="isAssignmentPending"
                  class="alert alert-warning mb-0"
                >
                  <div class="mb-2">
                    {{ t('assignmentPending') }}
                  </div>
                  <button
                    type="button"
                    class="btn btn-warning btn-sm"
                    :disabled="savingAccept"
                    @click="acceptAssignment"
                  >
                    <i class="bi bi-check2-circle me-1" />{{ savingAccept ? t('saving') : t('acceptAssignment') }}
                  </button>
                </div>
              </div>
            </div>

            <div class="card mb-4">
              <div class="card-header">
                <h3 class="card-title">
                  {{ t('actions') }}
                </h3>
              </div>
              <div class="card-body d-flex flex-column gap-2">
                <button
                  v-if="can('start')"
                  type="button"
                  class="btn btn-primary"
                  @click="openExecModal('start')"
                >
                  <i class="bi bi-play-circle me-2" />{{ t('start') }}
                </button>
                <button
                  v-if="can('wait-for-parts')"
                  type="button"
                  class="btn btn-outline-warning"
                  @click="openExecModal('wait-for-parts')"
                >
                  <i class="bi bi-box-seam me-2" />{{ t('waitForParts') }}
                </button>
                <button
                  v-if="can('resume')"
                  type="button"
                  class="btn btn-primary"
                  @click="openExecModal('resume')"
                >
                  <i class="bi bi-play-circle me-2" />{{ t('resume') }}
                </button>
                <button
                  v-if="can('complete')"
                  type="button"
                  class="btn btn-success"
                  @click="openExecModal('complete')"
                >
                  <i class="bi bi-check2-circle me-2" />{{ t('complete') }}
                </button>
                <button
                  v-if="can('cancel')"
                  type="button"
                  class="btn btn-outline-danger"
                  @click="openExecModal('cancel')"
                >
                  <i class="bi bi-x-circle me-2" />{{ t('cancelRequest') }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </template>
    </template>

    <AppModal
      id="exec-modal"
      :title="execAction === 'complete' ? t('complete') : execAction === 'cancel' ? t('cancelRequest') : execAction === 'wait-for-parts' ? t('waitForParts') : t('resume')"
    >
      <form @submit.prevent="runExec">
        <div class="mb-3">
          <label
            :for="`exec-${execAction}`"
            class="form-label"
          >
            {{ execAction === 'complete' ? t('workPerformed') : t('reason') }}
          </label>
          <textarea
            :id="`exec-${execAction}`"
            v-model="execForm"
            class="form-control"
            rows="3"
            :required="execAction !== 'resume'"
          />
        </div>
        <div
          v-if="execError"
          class="alert alert-danger py-2"
        >
          {{ execError }}
        </div>
      </form>
      <template #footer>
        <button
          type="button"
          class="btn btn-secondary"
          data-bs-dismiss="modal"
        >
          {{ t('cancel') }}
        </button>
        <button
          type="button"
          class="btn btn-primary"
          :disabled="savingExec"
          @click="runExec"
        >
          {{ savingExec ? t('saving') : t('save') }}
        </button>
      </template>
    </AppModal>
  </AppContent>
</template>
