<script setup lang="ts">
import { apiAssetUrl, getApiErrorCode, getApiErrorMessage } from '~/utils/api'
import { formatDate as formatApiDate } from '~/utils/date'
import { hideModal } from '~/utils/modal'
import type { AssignmentDetail, Attachment, RepairExecution, RepairRequest, StatusHistoryItem, Technician } from '~/types'
import type { RealtimeEvent } from '~/types/realtime'

const route = useRoute()
const id = Number(route.params.id)

const { t } = useLocale()
const realtime = useRealtime()

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

const activeTab = ref('general')
const assignForm = ref<number | ''>('')
const savingAssign = ref(false)
const message = ref('')
const actionError = ref('')
const deletingRequest = ref(false)

async function refreshRequestData() {
  await Promise.all([refresh(), refreshAssignments(), refreshAttachments(), refreshStatusHistory()])
  await refreshExecution()
}

const errorMessage = computed(() => {
  if (getApiErrorCode(error.value) === 'REPAIR_REQUEST_NOT_FOUND') return t('requestAlreadyDeleted')
  return getApiErrorMessage(error.value, 'Failed to load request.')
})
const isDeletedRequest = computed(() => getApiErrorCode(error.value) === 'REPAIR_REQUEST_NOT_FOUND')

async function deleteRequest() {
  deletingRequest.value = true
  actionError.value = ''
  try {
    await apiFetch(`/requests/${id}`, { method: 'DELETE' })
    await hideModal('request-delete-modal')
    await navigateTo('/admin/requests')
  } catch (e) {
    if (getApiErrorCode(e) === 'REPAIR_REQUEST_NOT_FOUND') {
      actionError.value = t('requestAlreadyDeleted')
      await hideModal('request-delete-modal')
      await navigateTo('/admin/requests')
    } else {
      actionError.value = getApiErrorMessage(e, 'Failed to delete request.')
    }
  } finally {
    deletingRequest.value = false
  }
}

let stopRealtime: (() => boolean) | undefined
onMounted(() => {
  stopRealtime = realtime.subscribe((event: RealtimeEvent) => {
    const payload = event.payload as { requestId?: number }
    const requestEvents = [
      'REQUEST_CREATED',
      'REQUEST_UPDATED',
      'REQUEST_ASSIGNED',
      'REQUEST_ASSIGNMENT_CREATED',
      'REQUEST_ASSIGNMENT_ACCEPTED',
      'REQUEST_ASSIGNMENT_REJECTED',
      'REQUEST_REASSIGNED',
      'REQUEST_UNASSIGNED',
      'REQUEST_SCHEDULE_CHANGED',
      'REQUEST_DIAGNOSIS_UPDATED',
      'REQUEST_ATTACHMENTS_CHANGED',
      'REQUEST_STATUS_CHANGED',
      'REQUEST_DELETED'
    ]
    if (payload.requestId === id && requestEvents.includes(event.type)) {
      void refreshRequestData()
    }
  })
})

onBeforeUnmount(() => stopRealtime?.())

const categoryName = computed(() => {
  const c = request.value?.category
  if (!c) return '-'
  return c.name || t('notSpecified')
})

const customerName = computed(() => request.value?.customer?.fullName || t('notSpecified'))
const customerPhone = computed(() => request.value?.customer?.phone || t('notSpecified'))
const locationAddress = computed(() => request.value?.location?.address || request.value?.address || t('notSpecified'))
const locationCoordinates = computed(() => {
  const latitude = request.value?.location?.latitude ?? request.value?.latitude
  const longitude = request.value?.location?.longitude ?? request.value?.longitude
  return latitude != null && longitude != null ? `${latitude}, ${longitude}` : '-'
})
const locationMapUrl = computed(() => {
  if (locationCoordinates.value === '-') return ''
  return `https://www.google.com/maps?q=${locationCoordinates.value}`
})
const assignedTechnician = computed(() => request.value?.currentAssignment?.technician ?? null)
const isAssignmentPending = computed(() =>
  request.value?.status === 'ASSIGNED' && request.value?.currentAssignment?.status === 'PENDING'
)

function formatDate(value?: string) {
  return formatApiDate(value, true)
}

function assignmentStatusLabel(status: string) {
  return t(`assignmentStatus.${status}`)
}

function assignmentStatusClass(status: string) {
  if (status === 'ACCEPTED' || status === 'COMPLETED') return 'text-bg-success'
  if (status === 'PENDING') return 'text-bg-warning'
  if (status === 'CANCELLED' || status === 'REJECTED' || status === 'UNASSIGNED') return 'text-bg-danger'
  return 'text-bg-secondary'
}

function historyReason(reason?: string) {
  if (!reason) return '-'
  const known: Record<string, string> = {
    'Technician assigned.': 'history.technicianAssigned',
    'Technician assigned': 'history.technicianAssigned',
    'Telegram request created.': 'history.telegramRequestCreated',
    'Telegram request created': 'history.telegramRequestCreated'
  }
  return known[reason] ? t(known[reason]) : reason
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
    :title="isDeletedRequest ? t('requestDeleted') : `#${request?.requestNumber || id}`"
    :breadcrumbs="[{ label: t('home'), to: '/admin' }, { label: t('requests'), to: '/admin/requests' }, ...(isDeletedRequest ? [] : [{ label: `#${request?.requestNumber || id}` }])]"
  >
    <div
      v-if="error && isDeletedRequest"
      class="text-center py-5"
    >
      <p class="text-danger mb-3">
        {{ t('requestAlreadyDeleted') }}
      </p>
      <NuxtLink
        to="/admin/requests"
        class="btn btn-outline-primary"
      >
        {{ t('backToRequests') }}
      </NuxtLink>
    </div>

    <div
      v-else-if="error"
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
            <ul class="nav nav-tabs mb-4">
              <li class="nav-item">
                <button
                  type="button"
                  class="nav-link"
                  :class="{ active: activeTab === 'general' }"
                  @click="activeTab = 'general'"
                >
                  {{ t('general') || 'Основное' }}
                </button>
              </li>
              <li class="nav-item">
                <button
                  type="button"
                  class="nav-link"
                  :class="{ active: activeTab === 'execution' }"
                  @click="activeTab = 'execution'"
                >
                  {{ t('executionDetails') }}
                </button>
              </li>
              <li class="nav-item">
                <button
                  type="button"
                  class="nav-link"
                  :class="{ active: activeTab === 'history' }"
                  @click="activeTab = 'history'"
                >
                  {{ t('history') || 'История' }}
                </button>
              </li>
              <li class="nav-item">
                <button
                  type="button"
                  class="nav-link"
                  :class="{ active: activeTab === 'actions' }"
                  @click="activeTab = 'actions'"
                >
                  {{ t('actions') || 'Действия' }}
                </button>
              </li>
            </ul>

            <div v-show="activeTab === 'general'">
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
                    {{ request.description || t('notSpecified') }}
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
                        class="text-body text-decoration-underline"
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
                      <span>{{ locationAddress }}</span>
                      <span
                        v-if="locationCoordinates !== '-'"
                        class="d-block small text-muted mt-1"
                      >
                        {{ t('coordinates') || 'Координаты' }}: {{ locationCoordinates }}
                      </span>
                      <a
                        v-if="locationMapUrl"
                        :href="locationMapUrl"
                        class="map-location-link d-inline-flex align-items-center gap-1 mt-2"
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        <i class="bi bi-geo-alt-fill" />
                        {{ t('openOnMap') }}
                        <i class="bi bi-box-arrow-up-right" />
                      </a>
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
                      <a
                        v-if="attachment.imagePreview && apiAssetUrl(attachment.downloadUrl)"
                        :href="apiAssetUrl(attachment.downloadUrl)"
                        class="attachment-preview"
                        target="_blank"
                        rel="noopener noreferrer"
                        :aria-label="attachment.originalFileName"
                      >
                        <img
                          :src="apiAssetUrl(attachment.downloadUrl)"
                          :alt="attachment.originalFileName"
                        >
                      </a>
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

            <div v-show="activeTab === 'execution'">
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
                      {{ execution.diagnosis || t('notSpecified') }}
                    </dd>
                    <dt class="col-sm-5">
                      {{ t('waitingReason') }}
                    </dt>
                    <dd class="col-sm-7">
                      {{ execution.waitingReason || t('notSpecified') }}
                    </dd>
                    <dt class="col-sm-5">
                      {{ t('workPerformed') }}
                    </dt>
                    <dd class="col-sm-7">
                      {{ execution.workPerformed || t('notSpecified') }}
                    </dd>
                    <dt class="col-sm-5">
                      {{ t('completionNote') }}
                    </dt>
                    <dd class="col-sm-7">
                      {{ execution.completionNote || t('notSpecified') }}
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

            <div v-show="activeTab === 'history'">
              <div class="row g-4">
                <div class="col-12">
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
                            :class="assignmentStatusClass(assignment.status)"
                          >{{ assignmentStatusLabel(assignment.status) }}</span>
                        </div>
                        <div class="small text-muted">
                          {{ formatDate(assignment.assignedAt) }} · {{ historyReason(assignment.rejectionReason || assignment.closureReason) }}
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
                <div class="col-12">
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
                          {{ historyReason(item.reason) }}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div v-show="activeTab === 'actions'">
              <div class="card mb-4">
                <div class="card-header">
                  <h3 class="card-title">
                    <i class="bi bi-grid me-2" />{{ t('actions') }}
                  </h3>
                </div>
                <div class="card-body">
                  <div class="row g-3">
                    <div class="col-md-6 col-lg-4">
                      <button
                        type="button"
                        class="btn btn-outline-danger w-100 h-100 d-flex flex-column align-items-center justify-content-center p-4 shadow-sm"
                        :disabled="deletingRequest"
                        @click="showModal('request-delete-modal')"
                      >
                        <i class="bi bi-trash fs-2 mb-2" />
                        <span class="fw-semibold">{{ t('deleteRequest') }}</span>
                      </button>
                    </div>

                    <div
                      v-if="can('diagnosis')"
                      class="col-md-6 col-lg-4"
                    >
                      <button
                        type="button"
                        class="btn btn-outline-info w-100 h-100 d-flex flex-column align-items-center justify-content-center p-4 shadow-sm"
                        @click="openExecModal('diagnosis')"
                      >
                        <i class="bi bi-search fs-2 mb-2" />
                        <span class="fw-semibold">{{ t('diagnosis') }}</span>
                      </button>
                    </div>

                    <div
                      v-if="can('start')"
                      class="col-md-6 col-lg-4"
                    >
                      <button
                        type="button"
                        class="btn btn-primary w-100 h-100 d-flex flex-column align-items-center justify-content-center p-4 shadow-sm"
                        @click="openExecModal('start')"
                      >
                        <i class="bi bi-play-circle fs-2 mb-2" />
                        <span class="fw-semibold">{{ t('start') }}</span>
                      </button>
                    </div>

                    <div
                      v-if="can('wait-for-parts')"
                      class="col-md-6 col-lg-4"
                    >
                      <button
                        type="button"
                        class="btn btn-outline-warning w-100 h-100 d-flex flex-column align-items-center justify-content-center p-4 shadow-sm"
                        @click="openExecModal('wait-for-parts')"
                      >
                        <i class="bi bi-box-seam fs-2 mb-2" />
                        <span class="fw-semibold">{{ t('waitForParts') }}</span>
                      </button>
                    </div>

                    <div
                      v-if="can('resume')"
                      class="col-md-6 col-lg-4"
                    >
                      <button
                        type="button"
                        class="btn btn-primary w-100 h-100 d-flex flex-column align-items-center justify-content-center p-4 shadow-sm"
                        @click="openExecModal('resume')"
                      >
                        <i class="bi bi-play-circle fs-2 mb-2" />
                        <span class="fw-semibold">{{ t('resume') }}</span>
                      </button>
                    </div>

                    <div
                      v-if="can('complete')"
                      class="col-md-6 col-lg-4"
                    >
                      <button
                        type="button"
                        class="btn btn-success w-100 h-100 d-flex flex-column align-items-center justify-content-center p-4 shadow-sm"
                        @click="openExecModal('complete')"
                      >
                        <i class="bi bi-check2-circle fs-2 mb-2" />
                        <span class="fw-semibold">{{ t('complete') }}</span>
                      </button>
                    </div>

                    <div
                      v-if="can('cancel')"
                      class="col-md-6 col-lg-4"
                    >
                      <button
                        type="button"
                        class="btn btn-outline-danger w-100 h-100 d-flex flex-column align-items-center justify-content-center p-4 shadow-sm"
                        @click="openExecModal('cancel')"
                      >
                        <i class="bi bi-x-circle fs-2 mb-2" />
                        <span class="fw-semibold">{{ t('cancelRequest') }}</span>
                      </button>
                    </div>
                  </div>
                </div>
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
                    {{ t(`priority.${request.priority}`) }}
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
            <ManagerChatBox
              v-if="request"
              :request-id="id"
              :request-status="request.status"
              class="mb-4"
            />
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

        <AppModal
          id="request-delete-modal"
          :title="t('deleteRequest')"
          size="sm"
        >
          <p class="mb-0">
            {{ t('confirmDeleteRequest') }}
          </p>
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
              class="btn btn-danger"
              :disabled="deletingRequest"
              @click="deleteRequest"
            >
              <span
                v-if="deletingRequest"
                class="spinner-border spinner-border-sm me-2"
              />
              {{ t('delete') }}
            </button>
          </template>
        </AppModal>
      </template>
    </template>
  </AppContent>
</template>
