<script setup lang="ts">
import { requestPriorities, requestStatuses } from '~/lib/statuses'
import type { Category, Page, RepairRequest, Technician } from '~/types'

const { t } = useLocale()
const search = ref('')
const status = ref('')
const priority = ref('')
const categoryId = ref('')
const page = ref(1)
const size = ref(10)

const query = computed(() => ({
  page: page.value - 1,
  size: size.value,
  search: search.value.trim() || undefined,
  status: status.value || undefined,
  priority: priority.value || undefined,
  categoryId: categoryId.value || undefined,
  sort: 'createdAt,desc'
}))

const { data, pending, error, refresh } = await useAsyncData('requests-list', () =>
  apiFetch<Page<RepairRequest>>('/requests', { query: query.value })
)

const { data: categories } = await useAsyncData('requests-categories', () =>
  apiFetch<Page<Category>>('/categories', { query: { size: 100 } })
)

const { data: technicians } = await useAsyncData('requests-technicians', () =>
  apiFetch<Page<Technician>>('/technicians', { query: { size: 100 } })
)

const rows = computed(() => data.value?.content ?? [])
const totalElements = computed(() => data.value?.totalElements ?? 0)
const totalPages = computed(() => data.value?.totalPages ?? 1)
const categoryOptions = computed(() => categories.value?.content ?? [])
const technicianOptions = computed(() => technicians.value?.content ?? [])

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

function categoryName(r: RepairRequest) {
  return r.category?.nameRu ?? r.category?.nameEn ?? '-'
}

function customerName(r: RepairRequest) {
  return r.customer?.fullName ?? '-'
}

function formatDate(value?: string) {
  return value ? new Date(value).toLocaleString() : '-'
}

const errorMessage = computed(() => {
  const err = error.value as { data?: { message?: string } } | null
  return err?.data?.message || error.value?.message || 'Unknown error.'
})

const statusRequestId = ref<number | null>(null)
const statusForm = ref('')
const savingStatus = ref(false)

function openStatusModal(r: RepairRequest) {
  statusRequestId.value = r.id
  statusForm.value = r.status
  showModal('status-modal')
}

async function saveStatus() {
  if (statusRequestId.value == null) return
  savingStatus.value = true
  try {
    const action = statusForm.value === 'COMPLETED'
      ? 'complete'
      : statusForm.value === 'CANCELLED'
        ? 'cancel'
        : statusForm.value === 'IN_PROGRESS'
          ? 'start'
          : statusForm.value === 'WAITING_FOR_PARTS'
            ? 'wait-for-parts'
            : null
    if (action) await apiFetch(`/requests/${statusRequestId.value}/${action}`, { method: 'POST' })
    hideModal('status-modal')
    refresh()
  } catch (e) {
    void e
  } finally {
    savingStatus.value = false
  }
}

const assignRequestId = ref<number | null>(null)
const assignForm = ref<number | ''>('')
const savingAssign = ref(false)

function openAssignModal(r: RepairRequest) {
  assignRequestId.value = r.id
  assignForm.value = ''
  showModal('assign-modal')
}

async function saveAssign() {
  if (assignRequestId.value == null || assignForm.value === '') return
  savingAssign.value = true
  try {
    await apiFetch(`/requests/${assignRequestId.value}/assign`, {
      method: 'POST',
      body: { technicianId: assignForm.value }
    })
    hideModal('assign-modal')
    refresh()
  } catch (e) {
    void e
  } finally {
    savingAssign.value = false
  }
}

const execAction = ref('')
const execRequestId = ref<number | null>(null)
const execForm = ref('')
const savingExec = ref(false)
const execError = ref('')

function openExecModal(action: string, r: RepairRequest) {
  execAction.value = action
  execRequestId.value = r.id
  execForm.value = ''
  execError.value = ''
  if (action === 'start') {
    runExec()
  } else {
    showModal('exec-modal')
  }
}

async function runExec() {
  if (execRequestId.value == null) return
  savingExec.value = true
  execError.value = ''
  try {
    const id = execRequestId.value
    const body = execAction.value === 'complete'
      ? { workPerformed: execForm.value }
      : execAction.value === 'wait-for-parts' || execAction.value === 'cancel'
        ? { reason: execForm.value }
        : execAction.value === 'resume'
          ? { note: execForm.value || undefined }
          : undefined
    await apiFetch(`/requests/${id}/${execAction.value}`, { method: 'POST', body })
    hideModal('exec-modal')
    refresh()
  } catch (e) {
    const err = e as { data?: { message?: string }, message?: string }
    execError.value = err.data?.message || err.message || 'Action failed.'
  } finally {
    savingExec.value = false
  }
}

const execTitle = computed(() => {
  if (execAction.value === 'wait-for-parts') return t('waitForParts')
  if (execAction.value === 'complete') return t('complete')
  if (execAction.value === 'cancel') return t('cancelRequest')
  if (execAction.value === 'resume') return t('resume')
  return ''
})

const execRequired = computed(() => execAction.value !== 'resume')
</script>

<template>
  <AppContent
    :title="t('requests')"
    :breadcrumbs="[{ label: t('home'), to: '/' }, { label: t('requests') }]"
  >
    <div class="card">
      <div class="card-header">
        <div class="d-flex flex-column flex-md-row gap-2 align-items-md-center justify-content-between">
          <h3 class="card-title mb-0">
            {{ t('allRequests') }}
          </h3>
          <div class="d-flex flex-column flex-sm-row gap-2 flex-wrap">
            <div
              class="input-group input-group-sm search-box"
            >
              <input
                v-model="search"
                type="search"
                class="form-control"
                :placeholder="t('searchRequests')"
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
            <select
              v-model="status"
              class="form-select form-select-sm"
              style="max-width: 180px;"
              @change="applyFilters"
            >
              <option value="">
                {{ t('allStatuses') }}
              </option>
              <option
                v-for="s in requestStatuses"
                :key="s.value"
                :value="s.value"
              >
                {{ t(`status.${s.value}`) }}
              </option>
            </select>
            <select
              v-model="priority"
              class="form-select form-select-sm"
              style="max-width: 140px;"
              @change="applyFilters"
            >
              <option value="">
                {{ t('all') }}
              </option>
              <option
                v-for="p in requestPriorities"
                :key="p.value"
                :value="p.value"
              >
                {{ p.value }}
              </option>
            </select>
            <select
              v-model="categoryId"
              class="form-select form-select-sm"
              style="max-width: 180px;"
              @change="applyFilters"
            >
              <option value="">
                {{ t('allCategories') }}
              </option>
              <option
                v-for="c in categoryOptions"
                :key="c.id"
                :value="c.id"
              >
                {{ c.nameRu || c.nameEn }}
              </option>
            </select>
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
              <th>#</th>
              <th>{{ t('requestNumber') }}</th>
              <th>{{ t('client') }}</th>
              <th>{{ t('categories') }}</th>
              <th>{{ t('priority') }}</th>
              <th>{{ t('status') }}</th>
              <th>{{ t('created') }}</th>
              <th class="text-end">
                {{ t('actions') }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="pending">
              <td
                colspan="8"
                class="text-center py-4"
              >
                <div class="spinner-border spinner-border-sm text-primary" />
              </td>
            </tr>
            <tr v-else-if="!rows.length">
              <td
                colspan="8"
                class="text-center py-4"
              >
                <div class="empty-state">
                  <i class="bi bi-wrench-adjustable" />
                  <p>{{ t('noRequestsFound') }}</p>
                </div>
              </td>
            </tr>
            <tr
              v-else
              v-for="r in rows"
              :key="r.id"
            >
              <td>
                <NuxtLink :to="`/requests/${r.id}`">
                  #{{ r.id }}
                </NuxtLink>
              </td>
              <td>
                <NuxtLink :to="`/requests/${r.id}`">
                  {{ r.requestNumber || `#${r.id}` }}
                </NuxtLink>
              </td>
              <td>{{ customerName(r) }}</td>
              <td>{{ categoryName(r) }}</td>
              <td>
                <span
                  class="badge"
                  :class="r.priority === 'URGENT' || r.priority === 'HIGH' ? 'text-bg-danger' : r.priority === 'LOW' ? 'text-bg-secondary' : 'text-bg-warning'"
                >
                  {{ r.priority }}
                </span>
              </td>
              <td><StatusBadge :status="r.status" /></td>
              <td class="text-nowrap">
                {{ formatDate(r.createdAt) }}
              </td>
              <td class="text-end text-nowrap">
                <div class="btn-group">
                  <NuxtLink
                    :to="`/requests/${r.id}`"
                    class="btn btn-sm btn-outline-secondary"
                    :title="t('view')"
                  >
                    <i class="bi bi-eye" />
                  </NuxtLink>
                  <button
                    type="button"
                    class="btn btn-sm btn-outline-secondary dropdown-toggle dropdown-toggle-split"
                    data-bs-toggle="dropdown"
                    aria-expanded="false"
                  >
                    <span class="visually-hidden">Toggle actions</span>
                  </button>
                  <ul class="dropdown-menu dropdown-menu-end">
                    <li>
                      <NuxtLink
                        class="dropdown-item"
                        :to="`/requests/${r.id}`"
                      >
                        <i class="bi bi-eye me-2" />{{ t('viewDetails') }}
                      </NuxtLink>
                    </li>
                    <li>
                      <a
                        href="#"
                        class="dropdown-item"
                        @click.prevent="openAssignModal(r)"
                      >
                        <i class="bi bi-person-badge me-2" />{{ t('assignTechnician') }}
                      </a>
                    </li>
                    <li><hr class="dropdown-divider"></li>
                    <li v-if="r.status === 'ASSIGNED'">
                      <a
                        href="#"
                        class="dropdown-item"
                        @click.prevent="openExecModal('start', r)"
                      >
                        <i class="bi bi-play-circle me-2" />{{ t('start') }}
                      </a>
                    </li>
                    <li v-if="r.status === 'IN_PROGRESS'">
                      <a
                        href="#"
                        class="dropdown-item"
                        @click.prevent="openExecModal('wait-for-parts', r)"
                      >
                        <i class="bi bi-box-seam me-2" />{{ t('waitForParts') }}
                      </a>
                    </li>
                    <li v-if="r.status === 'WAITING_FOR_PARTS'">
                      <a
                        href="#"
                        class="dropdown-item"
                        @click.prevent="openExecModal('resume', r)"
                      >
                        <i class="bi bi-play-circle me-2" />{{ t('resume') }}
                      </a>
                    </li>
                    <li v-if="r.status === 'IN_PROGRESS' || r.status === 'WAITING_FOR_PARTS'">
                      <a
                        href="#"
                        class="dropdown-item"
                        @click.prevent="openExecModal('complete', r)"
                      >
                        <i class="bi bi-check2-circle me-2" />{{ t('complete') }}
                      </a>
                    </li>
                    <li v-if="r.status !== 'COMPLETED' && r.status !== 'CANCELLED'">
                      <a
                        href="#"
                        class="dropdown-item text-danger"
                        @click.prevent="openExecModal('cancel', r)"
                      >
                        <i class="bi bi-x-circle me-2" />{{ t('cancelRequest') }}
                      </a>
                    </li>
                  </ul>
                </div>
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
        @update:page="goToPage"
        @update:size="changeSize"
      />
    </div>

    <AppModal
      id="assign-modal"
      :title="t('assignTechnician')"
    >
      <form @submit.prevent="saveAssign">
        <div class="mb-3">
          <label for="assign-select" class="form-label">{{ t('technician') }}</label>
          <select id="assign-select" v-model="assignForm" class="form-select" required>
            <option :value="''" disabled>
              {{ t('selectTechnician') || 'Select a technician...' }}
            </option>
            <option v-for="tech in technicianOptions" :key="tech.id" :value="tech.id">
              {{ tech.fullName }}
            </option>
          </select>
        </div>
      </form>
      <template #footer>
        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">{{ t('cancel') }}</button>
        <button type="button" class="btn btn-primary" :disabled="savingAssign || assignForm === ''" @click="saveAssign">
          {{ savingAssign ? t('saving') : t('save') }}
        </button>
      </template>
    </AppModal>

    <AppModal
      id="status-modal"
      :title="t('changeStatus')"
    >
      <div class="mb-3">
        <label for="status-select" class="form-label">{{ t('status') }}</label>
        <select id="status-select" v-model="statusForm" class="form-select">
          <option v-for="s in requestStatuses" :key="s.value" :value="s.value">
            {{ t(`status.${s.value}`) }}
          </option>
        </select>
      </div>
      <template #footer>
        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">{{ t('cancel') }}</button>
        <button type="button" class="btn btn-primary" :disabled="savingStatus" @click="saveStatus">
          {{ savingStatus ? t('saving') : t('save') }}
        </button>
      </template>
    </AppModal>

    <AppModal
      id="exec-modal"
      :title="execTitle"
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
            :required="execRequired"
          />
        </div>
        <div v-if="execError" class="alert alert-danger py-2">{{ execError }}</div>
      </form>
      <template #footer>
        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">{{ t('cancel') }}</button>
        <button type="button" class="btn btn-primary" :disabled="savingExec" @click="runExec">
          {{ savingExec ? t('saving') : t('save') }}
        </button>
      </template>
    </AppModal>
  </AppContent>
</template>
