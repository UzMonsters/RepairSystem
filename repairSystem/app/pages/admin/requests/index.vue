<script setup lang="ts">
import { requestPriorities, requestStatuses } from '~/lib/statuses'
import type { Category, Page, RepairRequest, Technician } from '~/types'
import { getApiErrorMessage } from '~/utils/api'
import { formatDate } from '~/utils/date'

const { t } = useLocale()
const route = useRoute()
const search = ref(typeof route.query.search === 'string' ? route.query.search : '')
const status = ref('')
const priority = ref('')
const categoryId = ref('')
const page = ref(1)
const size = ref(10)
const sortField = ref('createdAt')
const sortDirection = ref<'asc' | 'desc'>('desc')

const query = computed(() => ({
  page: page.value - 1,
  size: size.value,
  search: search.value.trim() || undefined,
  status: status.value || undefined,
  priority: priority.value || undefined,
  categoryId: categoryId.value || undefined,
  sort: `${sortField.value},${sortDirection.value}`
}))

const { data, pending, error, refresh } = await useAsyncData('requests-list', () =>
  apiFetch<Page<RepairRequest>>('/requests', { query: query.value }),
{ dedupe: 'defer' })

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

watch(() => route.query.search, (value) => {
  const nextSearch = typeof value === 'string' ? value : ''
  if (search.value === nextSearch) return
  search.value = nextSearch
  page.value = 1
  refresh()
})

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

function toggleSort(field: string) {
  if (sortField.value === field) {
    sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortField.value = field
    sortDirection.value = 'asc'
  }
  page.value = 1
  refresh()
}

function categoryName(r: RepairRequest) {
  return r.category?.name || '-'
}

function categoryOptionName(c: Category) {
  return c.name || '-'
}

function customerName(r: RepairRequest) {
  return r.customerFullName ?? r.customer?.fullName ?? '-'
}

function openRequest(r: RepairRequest) {
  navigateTo(`/admin/requests/${r.id}`)
}

function sortIcon(field: string) {
  return sortField.value === field
    ? sortDirection.value === 'asc' ? 'bi-arrow-up' : 'bi-arrow-down'
    : 'bi-arrow-down-up'
}

const errorMessage = computed(() => {
  return getApiErrorMessage(error.value, 'Unknown error.')
})

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
    await refresh()
  } catch (e) {
    actionError.value = getApiErrorMessage(e, 'Failed to assign technician.')
  } finally {
    savingAssign.value = false
  }
}

const execAction = ref('')
const execRequestId = ref<number | null>(null)
const execForm = ref('')
const savingExec = ref(false)
const execError = ref('')

const actionError = ref('')

async function acceptAssignment(r: RepairRequest) {
  actionError.value = ''
  try {
    await apiFetch(`/requests/${r.id}/assignment/accept`, { method: 'POST' })
    await refresh()
  } catch (e) {
    actionError.value = getApiErrorMessage(e, 'Failed to accept assignment.')
  }
}

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
    await refresh()
  } catch (e) {
    execError.value = getApiErrorMessage(e, 'Action failed.')
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
    :breadcrumbs="[{ label: t('home'), to: '/admin' }, { label: t('requests') }]"
  >
    <div class="card">
      <div class="card-header">
        <div class="d-flex flex-column flex-md-row gap-2 align-items-md-center justify-content-between">
          <h3 class="card-title mb-0">
            {{ t('allRequests') }}
          </h3>
          <div class="requests-filters d-flex flex-column flex-sm-row gap-2 flex-nowrap">
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
                {{ t(`priority.${p.value}`) }}
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
                {{ categoryOptionName(c) }}
              </option>
            </select>
          </div>
        </div>
      </div>

      <div class="card-body table-responsive p-0">
        <div
          v-if="actionError"
          class="alert alert-danger m-3"
        >
          {{ actionError }}
        </div>
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
              <th
                v-for="column in [
                  { label: t('description'), field: 'requestNumber' },
                  { label: t('client'), field: 'customerName' },
                  { label: t('categories'), field: 'categoryName' },
                  { label: t('priority'), field: 'priority' },
                  { label: t('status'), field: 'status' },
                  { label: t('created'), field: 'createdAt' }
                ]"
                :key="column.field"
              >
                <button
                  type="button"
                  class="table-sort-button"
                  @click.stop="toggleSort(column.field)"
                >
                  {{ column.label }}
                  <i
                    class="bi ms-1"
                    :class="sortIcon(column.field)"
                    aria-hidden="true"
                  />
                </button>
              </th>
              <th class="text-end">
                {{ t('actions') }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="pending">
              <td
                colspan="7"
                class="text-center py-4"
              >
                <div class="spinner-border spinner-border-sm text-primary" />
              </td>
            </tr>
            <tr v-else-if="!rows.length">
              <td
                colspan="7"
                class="text-center p-0"
              >
                <div class="empty-state table-empty-state">
                  <i class="bi bi-wrench-adjustable" />
                  <p>{{ t('noRequestsFound') }}</p>
                </div>
              </td>
            </tr>
            <tr
              v-for="r in rows"
              v-else
              :key="r.id"
              class="request-row-link"
              tabindex="0"
              @click="openRequest(r)"
              @keydown.enter="openRequest(r)"
            >
              <td class="request-description-cell">
                {{ r.description || categoryName(r) }}
              </td>
              <td>{{ customerName(r) }}</td>
              <td>{{ categoryName(r) }}</td>
              <td>
                <span
                  class="badge"
                  :class="r.priority === 'URGENT' || r.priority === 'HIGH' ? 'text-bg-danger' : r.priority === 'LOW' ? 'text-bg-secondary' : r.priority === 'NORMAL' ? 'priority-normal' : 'text-bg-warning'"
                >
                  {{ t(`priority.${r.priority}`) }}
                </span>
              </td>
              <td><StatusBadge :status="r.status" /></td>
              <td class="text-nowrap">
                {{ formatDate(r.createdAt, true) }}
              </td>
              <td class="text-end text-nowrap">
                <div
                  class="btn-group"
                  @click.stop
                >
                  <button
                    type="button"
                    class="btn btn-sm btn-outline-secondary"
                    :title="t('view')"
                    @click="openRequest(r)"
                  >
                    <i class="bi bi-eye" />
                  </button>
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
                        :to="`/admin/requests/${r.id}`"
                      >
                        <i class="bi bi-eye me-2" />{{ t('viewDetails') }}
                      </NuxtLink>
                    </li>
                    <li v-if="r.status !== 'COMPLETED' && r.status !== 'CANCELLED'">
                      <a
                        href="#"
                        class="dropdown-item"
                        @click.prevent="openAssignModal(r)"
                      >
                        <i class="bi bi-person-badge me-2" />{{ t('assignTechnician') }}
                      </a>
                    </li>
                    <li v-if="r.status !== 'COMPLETED' && r.status !== 'CANCELLED'">
                      <hr class="dropdown-divider">
                    </li>
                    <li v-if="r.status === 'ASSIGNED' && r.currentAssignment?.status === 'PENDING'">
                      <a
                        href="#"
                        class="dropdown-item"
                        @click.prevent="acceptAssignment(r)"
                      >
                        <i class="bi bi-check2-circle me-2" />{{ t('acceptAssignment') }}
                      </a>
                    </li>
                    <li v-if="r.status === 'ASSIGNED' && r.currentAssignment?.status === 'ACCEPTED'">
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
          <label
            for="assign-select"
            class="form-label"
          >{{ t('technician') }}</label>
          <select
            id="assign-select"
            v-model="assignForm"
            class="form-select"
            required
          >
            <option
              :value="''"
              disabled
            >
              {{ t('selectTechnician') || 'Select a technician...' }}
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
          :disabled="savingAssign || assignForm === ''"
          @click="saveAssign"
        >
          {{ savingAssign ? t('saving') : t('save') }}
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
