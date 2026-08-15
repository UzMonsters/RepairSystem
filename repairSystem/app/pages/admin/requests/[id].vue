<script setup lang="ts">
import { getApiErrorCode, getApiErrorMessage } from '~/utils/api'
import { formatDate as formatApiDate } from '~/utils/date'
import type { AssignmentDetail, Attachment, RepairExecution, RepairRequest, StatusHistoryItem, Technician } from '~/types'

const route = useRoute()
const id = Number(route.params.id)

const { t } = useLocale()

const { data: request, pending, error, refresh } = await useAsyncData(`request-${id}`, () =>
  apiFetch<RepairRequest>(`/requests/${id}`)
)

const { data: technicians } = await useAsyncData(`request-${id}-technicians`, () =>
  apiFetch<{ content: Technician[] }>('/technicians', { query: { size: 100 } })
)

const { data: assignments, refresh: refreshAssignments } = await useAsyncData(`request-${id}-assignments`, () =>
  apiFetch<AssignmentDetail[]>(`/requests/${id}/assignments`)
)

const { data: attachments, refresh: refreshAttachments } = await useAsyncData(`request-${id}-attachments`, () =>
  apiFetch<Attachment[]>(`/requests/${id}/attachments`)
)

const execution = ref<RepairExecution | null>(null)
const executionStatuses = new Set(['IN_PROGRESS', 'WAITING_FOR_PARTS', 'COMPLETED', 'CANCELLED'])

async function refreshExecution() {
  if (!executionStatuses.has(request.value?.status ?? '')) {
    execution.value = null
    return
  }

  try {
    execution.value = await apiFetch<RepairExecution>(`/requests/${id}/execution`)
  } catch (e) {
    if (getApiErrorCode(e) === 'REPAIR_EXECUTION_NOT_FOUND') {
      execution.value = null
      return
    }
    throw e
  }
}

await refreshExecution()

const { data: statusHistory, refresh: refreshStatusHistory } = await useAsyncData(`request-${id}-status-history`, () =>
  apiFetch<StatusHistoryItem[]>(`/requests/${id}/status-history`)
)

const technicianOptions = computed(() => technicians.value?.content ?? [])

const assignForm = ref<number | ''>('')
const savingAssign = ref(false)
const message = ref('')
const actionError = ref('')

const scheduleForm = ref('')
const savingSchedule = ref(false)
const scheduleInput = ref<HTMLInputElement | null>(null)

async function refreshRequestData() {
  await Promise.all([refresh(), refreshAssignments(), refreshAttachments(), refreshStatusHistory()])
  await refreshExecution()
}

const errorMessage = computed(() => {
  return getApiErrorMessage(error.value, 'Failed to load request.')
})

const categoryName = computed(() => {
  const c = request.value?.category
  if (!c) return '-'
  return c.name || '-'
})

const customerName = computed(() => request.value?.customer?.fullName || '-')
const customerPhone = computed(() => request.value?.customer?.phone || '-')
const assignedTechnician = computed(() => request.value?.currentAssignment?.technician ?? null)
const isAssignmentPending = computed(() =>
  request.value?.status === 'ASSIGNED' && request.value?.currentAssignment?.status === 'PENDING'
)

function formatDate(value?: string) {
  return formatApiDate(value, true)
}

const savingAccept = ref(false)

async function acceptAssignment() {
  savingAccept.value = true
  try {
    await apiFetch(`/requests/${id}/assignment/accept`, { method: 'POST' })
    message.value = t('statusUpdated')
    await refreshRequestData()
  } catch (e) {
    message.value = getApiErrorMessage(e, 'Failed to accept assignment.')
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
    await refreshRequestData()
  } catch (e) {
    message.value = getApiErrorMessage(e, 'Failed to assign technician.')
  } finally {
    savingAssign.value = false
  }
}

const assignmentAction = ref<'reassign' | 'unassign' | ''>('')
const assignmentReason = ref('')
const savingAssignmentAction = ref(false)

function openAssignmentAction(action: 'reassign' | 'unassign') {
  assignmentAction.value = action
  assignmentReason.value = ''
  actionError.value = ''
  showModal('assignment-action-modal')
}

async function runAssignmentAction() {
  if (!assignmentAction.value || !assignmentReason.value.trim()) return
  savingAssignmentAction.value = true
  actionError.value = ''
  try {
    const body = assignmentAction.value === 'reassign'
      ? { technicianId: assignForm.value, reason: assignmentReason.value.trim() }
      : { reason: assignmentReason.value.trim() }
    if (assignmentAction.value === 'reassign' && assignForm.value === '') return
    await apiFetch(`/requests/${id}/${assignmentAction.value}`, { method: 'POST', body })
    hideModal('assignment-action-modal')
    message.value = t('assignmentUpdated')
    await refreshRequestData()
  } catch (e) {
    actionError.value = getApiErrorMessage(e, 'Failed to update assignment.')
  } finally {
    savingAssignmentAction.value = false
  }
}

async function saveSchedule(clearSchedule = false) {
  if (!clearSchedule && !scheduleForm.value) return
  savingSchedule.value = true
  actionError.value = ''
  try {
    await apiFetch(`/requests/${id}/schedule`, {
      method: 'PATCH',
      body: clearSchedule
        ? { clearSchedule: true }
        : { scheduledVisitAt: new Date(scheduleForm.value).toISOString() }
    })
    message.value = t('scheduleUpdated')
    scheduleForm.value = ''
    await refreshRequestData()
  } catch (e) {
    actionError.value = getApiErrorMessage(e, 'Failed to update schedule.')
  } finally {
    savingSchedule.value = false
  }
}

const attachmentType = ref<'CUSTOMER_PROBLEM_PHOTO' | 'DIAGNOSIS_PHOTO' | 'COMPLETION_PHOTO' | 'GENERAL_DOCUMENT'>('GENERAL_DOCUMENT')
const attachmentFile = ref<File | null>(null)
const uploadingAttachment = ref(false)

function selectAttachment(event: Event) {
  attachmentFile.value = (event.target as HTMLInputElement).files?.[0] ?? null
}

async function uploadAttachment() {
  if (!attachmentFile.value) return
  uploadingAttachment.value = true
  actionError.value = ''
  try {
    const formData = new FormData()
    formData.append('type', attachmentType.value)
    formData.append('file', attachmentFile.value)
    await apiFetch(`/requests/${id}/attachments`, { method: 'POST', body: formData })
    attachmentFile.value = null
    message.value = t('attachmentUploaded')
    await refreshAttachments()
  } catch (e) {
    actionError.value = getApiErrorMessage(e, 'Failed to upload attachment.')
  } finally {
    uploadingAttachment.value = false
  }
}

async function downloadAttachment(attachment: Attachment) {
  try {
    const file = await apiFetch<Blob>(`/attachments/${attachment.id}/download`, { responseType: 'blob' })
    const url = URL.createObjectURL(file)
    const link = document.createElement('a')
    link.href = url
    link.download = attachment.originalFileName || `attachment-${attachment.id}`
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
  } catch (e) {
    actionError.value = getApiErrorMessage(e, 'Failed to create download link.')
  }
}

async function deleteAttachment(attachment: Attachment) {
  try {
    await apiFetch(`/attachments/${attachment.id}`, { method: 'DELETE', body: {} })
    message.value = t('attachmentDeleted')
    await refreshAttachments()
  } catch (e) {
    actionError.value = getApiErrorMessage(e, 'Failed to delete attachment.')
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
      : execAction.value === 'diagnosis'
        ? { diagnosis: execForm.value }
        : execAction.value === 'wait-for-parts' || execAction.value === 'cancel'
          ? { reason: execForm.value }
          : execAction.value === 'resume'
            ? { note: execForm.value || undefined }
            : undefined
    await apiFetch(`/requests/${id}/${execAction.value}`, {
      method: execAction.value === 'diagnosis' ? 'PATCH' : 'POST',
      body
    })
    hideModal('exec-modal')
    message.value = t('statusUpdated')
    await refreshRequestData()
  } catch (e) {
    execError.value = getApiErrorMessage(e, 'Action failed.')
  } finally {
    savingExec.value = false
  }
}

function can(action: string) {
  const s = request.value?.status
  if (action === 'start') return s === 'ASSIGNED' && request.value?.currentAssignment?.status === 'ACCEPTED'
  if (action === 'diagnosis') return s === 'IN_PROGRESS' || s === 'WAITING_FOR_PARTS'
  if (action === 'wait-for-parts' || action === 'complete') return s === 'IN_PROGRESS' || s === 'WAITING_FOR_PARTS'
  if (action === 'resume') return s === 'WAITING_FOR_PARTS'
  if (action === 'cancel') return s !== 'COMPLETED' && s !== 'CANCELLED'
  return false
}
</script>

<template>
  <AppContent
    :title="`#${request?.requestNumber || id}`"
    :breadcrumbs="[{ label: t('home'), to: '/admin' }, { label: t('requests'), to: '/admin/requests' }, { label: `#${request?.requestNumber || id}` }]"
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
        <div
          v-if="actionError"
          class="alert alert-danger py-2"
        >
          {{ actionError }}
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
                      :to="`/admin/customers/${request.customer.id}`"
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
                  v-if="request.currentAssignment"
                  class="d-flex flex-wrap gap-2 mt-3"
                >
                  <button
                    type="button"
                    class="btn btn-outline-primary btn-sm"
                    @click="openAssignmentAction('reassign')"
                  >
                    <i class="bi bi-arrow-repeat me-1" />{{ t('reassignTechnician') }}
                  </button>
                  <button
                    type="button"
                    class="btn btn-outline-danger btn-sm"
                    @click="openAssignmentAction('unassign')"
                  >
                    <i class="bi bi-person-dash me-1" />{{ t('unassignTechnician') }}
                  </button>
                </div>
                <div
                  v-if="request.currentAssignment"
                  class="mt-3"
                >
                  <label class="form-label">{{ t('scheduledVisitAt') }}</label>
                  <div class="input-group">
                    <input
                      ref="scheduleInput"
                      v-model="scheduleForm"
                      type="datetime-local"
                      class="form-control"
                      :disabled="savingSchedule"
                      @click="scheduleInput?.showPicker?.()"
                    >
                    <button
                      type="button"
                      class="btn btn-outline-primary"
                      :disabled="savingSchedule || !scheduleForm"
                      @click="saveSchedule()"
                    >
                      {{ t('save') }}
                    </button>
                    <button
                      type="button"
                      class="btn btn-outline-secondary"
                      :disabled="savingSchedule || !request.currentAssignment.scheduledVisitAt"
                      @click="saveSchedule(true)"
                    >
                      {{ t('clear') }}
                    </button>
                  </div>
                  <div class="small text-muted mt-1">
                    {{ request.currentAssignment.scheduledVisitAt ? formatDate(request.currentAssignment.scheduledVisitAt) : t('notScheduled') }}
                  </div>
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
                  v-if="can('diagnosis')"
                  type="button"
                  class="btn btn-outline-info"
                  @click="openExecModal('diagnosis')"
                >
                  <i class="bi bi-clipboard2-pulse me-2" />{{ t('diagnosis') }}
                </button>
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

    <div class="row g-4 mt-1">
      <div class="col-lg-6">
        <div class="card h-100">
          <div class="card-header">
            <h3 class="card-title mb-0">
              {{ t('attachments') }}
            </h3>
          </div>
          <div class="card-body">
            <div class="attachment-upload-grid mb-3">
              <div class="col-md-5">
                <select
                  v-model="attachmentType"
                  class="form-select"
                >
                  <option value="GENERAL_DOCUMENT">
                    {{ t('generalDocument') }}
                  </option>
                  <option value="CUSTOMER_PROBLEM_PHOTO">
                    {{ t('customerProblemPhoto') }}
                  </option>
                  <option value="DIAGNOSIS_PHOTO">
                    {{ t('diagnosisPhoto') }}
                  </option>
                  <option value="COMPLETION_PHOTO">
                    {{ t('completionPhoto') }}
                  </option>
                </select>
              </div>
              <div class="col-md-7 attachment-file-column">
                <div class="file-input-control">
                  <input
                    id="request-attachment-file"
                    type="file"
                    class="visually-hidden"
                    @change="selectAttachment"
                  >
                  <label
                    for="request-attachment-file"
                    class="file-input-button"
                  >
                    <i class="bi bi-folder2-open me-1" />{{ t('chooseFile') }}
                  </label>
                  <span
                    class="file-input-name"
                    :class="{ 'is-empty': !attachmentFile }"
                  >
                    {{ attachmentFile?.name || t('noFileChosen') }}
                  </span>
                </div>
              </div>
            </div>
            <button
              type="button"
              class="btn btn-primary btn-sm attachment-upload-button mb-3"
              :disabled="uploadingAttachment || !attachmentFile"
              @click="uploadAttachment"
            >
              <i class="bi bi-upload me-1" />{{ uploadingAttachment ? t('saving') : t('upload') }}
            </button>
            <div
              v-if="!attachments?.length"
              class="text-muted"
            >
              {{ t('noAttachments') }}
            </div>
            <div
              v-for="attachment in attachments"
              :key="attachment.id"
              class="attachment-list-item"
            >
              <div class="attachment-info">
                <div class="fw-semibold attachment-file-name">
                  {{ attachment.originalFileName }}
                </div>
                <div class="small text-muted attachment-meta">
                  {{ t(`attachmentType.${attachment.type}`) }} · {{ formatDate(attachment.uploadedAt) }}
                </div>
              </div>
              <div class="btn-group btn-group-sm attachment-actions">
                <button
                  type="button"
                  class="btn btn-outline-primary"
                  :title="t('download')"
                  @click="downloadAttachment(attachment)"
                >
                  <i class="bi bi-download" />
                </button>
                <button
                  type="button"
                  class="btn btn-outline-danger"
                  :title="t('delete')"
                  @click="deleteAttachment(attachment)"
                >
                  <i class="bi bi-trash" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="col-lg-6">
        <div class="card h-100">
          <div class="card-header">
            <h3 class="card-title mb-0">
              {{ t('executionDetails') }}
            </h3>
          </div>
          <div class="card-body">
            <dl
              v-if="execution"
              class="row mb-0"
            >
              <dt class="col-sm-5">
                {{ t('diagnosis') }}
              </dt>
              <dd class="col-sm-7">
                {{ execution.diagnosis || '-' }}
              </dd>
              <dt class="col-sm-5">
                {{ t('waitingReason') }}
              </dt>
              <dd class="col-sm-7">
                {{ execution.waitingReason || '-' }}
              </dd>
              <dt class="col-sm-5">
                {{ t('workPerformed') }}
              </dt>
              <dd class="col-sm-7">
                {{ execution.workPerformed || '-' }}
              </dd>
              <dt class="col-sm-5">
                {{ t('completionNote') }}
              </dt>
              <dd class="col-sm-7">
                {{ execution.completionNote || '-' }}
              </dd>
              <dt class="col-sm-5">
                {{ t('completed') }}
              </dt>
              <dd class="col-sm-7">
                {{ formatDate(execution.completedAt) }}
              </dd>
            </dl>
            <div
              v-else
              class="text-muted"
            >
              {{ t('noExecutionDetails') }}
            </div>
          </div>
        </div>
      </div>

      <div class="col-lg-6">
        <div class="card">
          <div class="card-header">
            <h3 class="card-title mb-0">
              {{ t('assignmentHistory') }}
            </h3>
          </div>
          <div class="card-body p-0">
            <div
              v-if="!assignments?.length"
              class="text-muted p-3"
            >
              {{ t('noAssignmentHistory') }}
            </div>
            <div
              v-for="assignment in assignments"
              :key="assignment.id"
              class="border-bottom p-3"
            >
              <div class="d-flex justify-content-between gap-2">
                <strong>{{ assignment.technician.fullName }}</strong>
                <span
                  class="badge"
                  :class="assignment.status === 'COMPLETED' ? 'text-bg-success' : 'text-bg-secondary'"
                >{{ assignment.status }}</span>
              </div>
              <div class="small text-muted">
                {{ formatDate(assignment.assignedAt) }} В· {{ assignment.rejectionReason || assignment.closureReason || '-' }}
              </div>
              <div
                v-if="assignment.scheduledVisitAt"
                class="small"
              >
                {{ t('scheduledVisitAt') }}: {{ formatDate(assignment.scheduledVisitAt) }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="col-lg-6">
        <div class="card">
          <div class="card-header">
            <h3 class="card-title mb-0">
              {{ t('statusHistory') }}
            </h3>
          </div>
          <div class="card-body p-0">
            <div
              v-if="!statusHistory?.length"
              class="text-muted p-3"
            >
              {{ t('noStatusHistory') }}
            </div>
            <div
              v-for="item in statusHistory"
              :key="item.id"
              class="border-bottom p-3"
            >
              <div class="d-flex align-items-center gap-2">
                <StatusBadge :status="item.toStatus" />
                <span class="small text-muted">{{ formatDate(item.changedAt) }}</span>
              </div>
              <div
                v-if="item.reason"
                class="small mt-1"
              >
                {{ item.reason }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <AppModal
      id="assignment-action-modal"
      :title="assignmentAction === 'reassign' ? t('reassignTechnician') : t('unassignTechnician')"
    >
      <form @submit.prevent="runAssignmentAction">
        <div
          v-if="assignmentAction === 'reassign'"
          class="mb-3"
        >
          <label class="form-label">{{ t('technician') }}</label>
          <select
            v-model="assignForm"
            class="form-select"
          >
            <option
              value=""
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
        </div>
        <label class="form-label">{{ t('reason') }}</label>
        <textarea
          v-model="assignmentReason"
          class="form-control"
          rows="3"
          required
        />
        <div
          v-if="actionError"
          class="alert alert-danger py-2 mt-3"
        >
          {{ actionError }}
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
          :disabled="savingAssignmentAction || !assignmentReason.trim() || (assignmentAction === 'reassign' && assignForm === '')"
          @click="runAssignmentAction"
        >
          {{ savingAssignmentAction ? t('saving') : t('save') }}
        </button>
      </template>
    </AppModal>

    <AppModal
      id="exec-modal"
      :title="execAction === 'diagnosis' ? t('diagnosis') : execAction === 'complete' ? t('complete') : execAction === 'cancel' ? t('cancelRequest') : execAction === 'wait-for-parts' ? t('waitForParts') : t('resume')"
    >
      <form @submit.prevent="runExec">
        <div class="mb-3">
          <label
            :for="`exec-${execAction}`"
            class="form-label"
          >
            {{ execAction === 'complete' ? t('workPerformed') : execAction === 'diagnosis' ? t('diagnosis') : t('reason') }}
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
