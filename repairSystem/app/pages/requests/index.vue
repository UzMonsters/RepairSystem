<script setup lang="ts">
import { requestStatuses } from '~/lib/statuses'
import type { Category, Page, RepairRequest, RequestStatus, Technician } from '~/types'

const { t } = useLocale()
const search = ref('')
const status = ref('')
const categoryId = ref('')
const page = ref(1)
const size = ref(10)

const query = computed(() => ({
  page: page.value,
  size: size.value,
  search: search.value.trim() || undefined,
  status: status.value || undefined,
  categoryId: categoryId.value || undefined
}))

const { data, pending, error, refresh } = await useAsyncData('requests-list', () =>
  apiFetch<Page<RepairRequest>>('/requests', { query: query.value })
)

const { data: categories } = await useAsyncData('requests-categories', () =>
  apiFetch<Category[]>('/categories')
)

const { data: technicians } = await useAsyncData('requests-technicians', () =>
  apiFetch<Technician[]>('/technicians')
)

const currentPage = computed(() => data.value?.page ?? 1)
const totalElements = computed(() => data.value?.totalElements ?? 0)
const totalPages = computed(() => Math.max(1, Math.ceil(totalElements.value / size.value)))
const startIndex = computed(() => (totalElements.value === 0 ? 0 : (currentPage.value - 1) * size.value + 1))
const endIndex = computed(() => Math.min(currentPage.value * size.value, totalElements.value))

function applyFilters() {
  page.value = 1
  refresh()
}

function goToPage(target: number) {
  page.value = target
  refresh()
}

function categoryName(r: RepairRequest) {
  return typeof r.category === 'string' ? r.category : r.category?.name ?? '-'
}

function customerName(r: RepairRequest) {
  return r.customer?.name || '-'
}

function formatDate(value?: string) {
  return value ? new Date(value).toLocaleString() : '-'
}

const errorMessage = computed(() => {
  const err = error.value as { data?: { message?: string } } | null
  return err?.data?.message || error.value?.message || 'Unknown error.'
})

const statusRequestId = ref<number | null>(null)
const statusForm = ref<RequestStatus>('NEW')
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
          : statusForm.value === 'WAITING_PARTS'
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
  assignForm.value = r.technicianId ?? ''
  showModal('assign-modal')
}

async function saveAssign() {
  if (assignRequestId.value == null || assignForm.value === '') return
  savingAssign.value = true
  try {
    await apiFetch(`/requests/${assignRequestId.value}/assign`, { method: 'POST', body: { technicianId: assignForm.value } })
    hideModal('assign-modal')
    refresh()
  } catch (e) {
    void e
  } finally {
    savingAssign.value = false
  }
}

async function removeRequest(r: RepairRequest) {
  if (!confirm(`Delete request #${r.id}? This action cannot be undone.`)) return
  try {
    await apiFetch(`/requests/${r.id}`, { method: 'DELETE' })
    refresh()
  } catch (e) {
    void e
  }
}
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
            All Requests
          </h3>
          <div class="d-flex flex-column flex-sm-row gap-2">
            <div
              class="input-group input-group-sm"
              style="max-width: 260px;"
            >
              <input
                v-model="search"
                type="search"
                class="form-control"
                placeholder="Search requests..."
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
                All Statuses
              </option>
              <option
                v-for="s in requestStatuses"
                :key="s.value"
                :value="s.value"
              >
                {{ s.label }}
              </option>
            </select>
            <select
              v-model="categoryId"
              class="form-select form-select-sm"
              style="max-width: 180px;"
              @change="applyFilters"
            >
              <option value="">
                All Categories
              </option>
              <option
                v-for="c in categories"
                :key="c.id"
                :value="c.id"
              >
                {{ c.name }}
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
          Failed to load requests. {{ errorMessage }}
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
              <th>#</th>
              <th>Customer</th>
              <th>Category</th>
              <th>Status</th>
              <th>Technician</th>
              <th>Created</th>
              <th class="text-end">
                Actions
              </th>
            </tr>
          </thead>
          <tbody>
            <template v-if="pending">
              <tr>
                <td
                  colspan="7"
                  class="text-center py-4"
                >
                  <div class="spinner-border spinner-border-sm text-primary" />
                </td>
              </tr>
            </template>
            <template v-else-if="!data?.content.length">
              <tr>
                <td
                  colspan="7"
                  class="text-center text-muted py-4"
                >
                  No requests found.
                </td>
              </tr>
            </template>
            <template v-else>
              <tr
                v-for="r in data?.content"
                :key="r.id"
              >
                <td>
                  <NuxtLink :to="`/requests/${r.id}`">
                    #{{ r.id }}
                  </NuxtLink>
                </td>
                <td>{{ customerName(r) }}</td>
                <td>{{ categoryName(r) }}</td>
                <td><StatusBadge :status="r.status" /></td>
                <td>{{ r.technician?.fullName || '-' }}</td>
                <td class="text-nowrap">
                  {{ formatDate(r.createdAt) }}
                </td>
                <td class="text-end text-nowrap">
                  <div class="btn-group">
                    <NuxtLink
                      :to="`/requests/${r.id}`"
                      class="btn btn-sm btn-outline-secondary"
                      title="View"
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
                          <i class="bi bi-eye me-2" />View Details
                        </NuxtLink>
                      </li>
                      <li>
                        <a
                          href="#"
                          class="dropdown-item"
                          @click.prevent="openStatusModal(r)"
                        >
                          <i class="bi bi-arrow-repeat me-2" />Change Status
                        </a>
                      </li>
                      <li>
                        <a
                          href="#"
                          class="dropdown-item"
                          @click.prevent="openAssignModal(r)"
                        >
                          <i class="bi bi-person-badge me-2" />Assign Technician
                        </a>
                      </li>
                      <li><hr class="dropdown-divider"></li>
                      <li>
                        <a
                          href="#"
                          class="dropdown-item text-danger"
                          @click.prevent="removeRequest(r)"
                        >
                          <i class="bi bi-trash me-2" />Delete
                        </a>
                      </li>
                    </ul>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <div
        v-if="!error"
        class="card-footer"
      >
        <div class="d-flex justify-content-between align-items-center">
          <span class="text-muted small">
            Showing {{ startIndex }}–{{ endIndex }} of {{ totalElements }}
          </span>
          <ul class="pagination pagination-sm mb-0">
            <li
              class="page-item"
              :class="{ disabled: currentPage <= 1 }"
            >
              <a
                href="#"
                class="page-link"
                @click.prevent="goToPage(currentPage - 1)"
              >&laquo;</a>
            </li>
            <li class="page-item active">
              <span class="page-link">{{ currentPage }} / {{ totalPages }}</span>
            </li>
            <li
              class="page-item"
              :class="{ disabled: currentPage >= totalPages }"
            >
              <a
                href="#"
                class="page-link"
                @click.prevent="goToPage(currentPage + 1)"
              >&raquo;</a>
            </li>
          </ul>
        </div>
      </div>
    </div>

    <AppModal
      id="status-modal"
      title="Change Request Status"
    >
      <div class="mb-3">
        <label
          for="status-select"
          class="form-label"
        >Status</label>
        <select
          id="status-select"
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
      </div>
      <template #footer>
        <button
          type="button"
          class="btn btn-secondary"
          data-bs-dismiss="modal"
        >
          Cancel
        </button>
        <button
          type="button"
          class="btn btn-primary"
          :disabled="savingStatus"
          @click="saveStatus"
        >
          {{ savingStatus ? 'Saving...' : 'Save' }}
        </button>
      </template>
    </AppModal>

    <AppModal
      id="assign-modal"
      title="Assign Technician"
    >
      <div class="mb-3">
        <label
          for="assign-select"
          class="form-label"
        >Technician</label>
        <select
          id="assign-select"
          v-model="assignForm"
          class="form-select"
        >
          <option
            :value="''"
            disabled
          >
            Select a technician...
          </option>
          <option
            v-for="t in technicians"
            :key="t.id"
            :value="t.id"
          >
            {{ t.fullName }}
          </option>
        </select>
      </div>
      <template #footer>
        <button
          type="button"
          class="btn btn-secondary"
          data-bs-dismiss="modal"
        >
          Cancel
        </button>
        <button
          type="button"
          class="btn btn-primary"
          :disabled="savingAssign || assignForm === ''"
          @click="saveAssign"
        >
          {{ savingAssign ? 'Saving...' : 'Assign' }}
        </button>
      </template>
    </AppModal>
  </AppContent>
</template>
