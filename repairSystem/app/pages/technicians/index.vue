<script setup lang="ts">
import type { RepairRequest, Technician } from '~/types'

const search = ref('')

const { data, pending, error, refresh } = await useAsyncData('technicians-list', () =>
  apiFetch<Technician[]>('/technicians')
)

const errorMessage = computed(() => {
  const err = error.value as { data?: { message?: string } } | null
  return err?.data?.message || error.value?.message || 'Failed to load technicians.'
})

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return data.value ?? []
  return (data.value ?? []).filter(t =>
    t.fullName.toLowerCase().includes(q) || t.phone.toLowerCase().includes(q)
  )
})

const editingId = ref<number | null>(null)
const form = ref({ fullName: '', phone: '' })
const saving = ref(false)

function openCreate() {
  editingId.value = null
  form.value = { fullName: '', phone: '' }
  showModal('technician-modal')
}

function openEdit(t: Technician) {
  editingId.value = t.id
  form.value = { fullName: t.fullName, phone: t.phone }
  showModal('technician-modal')
}

async function save() {
  saving.value = true
  try {
    if (editingId.value == null) {
      await apiFetch('/technicians', { method: 'POST', body: form.value })
    } else {
      await apiFetch(`/technicians/${editingId.value}`, { method: 'PATCH', body: form.value })
    }
    hideModal('technician-modal')
    refresh()
  } catch (e) {
    void e
  } finally {
    saving.value = false
  }
}

async function toggleActive(t: Technician) {
  try {
    await apiFetch(`/technicians/${t.id}`, { method: 'PATCH', body: { active: !t.active } })
    refresh()
  } catch (e) {
    void e
  }
}

async function removeTechnician(t: Technician) {
  if (!confirm(`Delete technician "${t.fullName}"? This action cannot be undone.`)) return
  try {
    await apiFetch(`/technicians/${t.id}`, { method: 'DELETE' })
    refresh()
  } catch (e) {
    void e
  }
}

const viewingId = ref<number | null>(null)
const assigned = ref<RepairRequest[]>([])
const loadingAssigned = ref(false)
const assignedError = ref('')

async function openAssigned(t: Technician) {
  viewingId.value = t.id
  assigned.value = []
  assignedError.value = ''
  showModal('assigned-modal')
  loadingAssigned.value = true
  try {
    assigned.value = await apiFetch<RepairRequest[]>(`/technicians/${t.id}/requests`)
  } catch (e) {
    const err = e as { data?: { message?: string }, message?: string }
    assignedError.value = err.data?.message || err.message || 'Failed to load requests.'
  } finally {
    loadingAssigned.value = false
  }
}

function categoryName(r: RepairRequest) {
  return typeof r.category === 'string' ? r.category : r.category?.name ?? '-'
}

function formatDate(value?: string) {
  return value ? new Date(value).toLocaleDateString() : '-'
}
</script>

<template>
  <AppContent
    title="Technicians"
    :breadcrumbs="[{ label: 'Home', to: '/' }, { label: 'Technicians' }]"
  >
    <div class="card">
      <div class="card-header">
        <div class="d-flex flex-column flex-md-row gap-2 align-items-md-center justify-content-between">
          <h3 class="card-title mb-0">
            Technicians
          </h3>
          <div class="d-flex gap-2">
            <div
              class="input-group input-group-sm"
              style="max-width: 240px;"
            >
              <input
                v-model="search"
                type="search"
                class="form-control"
                placeholder="Search..."
              >
              <button
                type="button"
                class="btn btn-outline-secondary"
              >
                <i class="bi bi-search" />
              </button>
            </div>
            <button
              type="button"
              class="btn btn-sm btn-primary"
              @click="openCreate"
            >
              <i class="bi bi-plus-lg me-1" />New Technician
            </button>
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
            Retry
          </button>
        </div>

        <table
          v-else
          class="table table-striped table-hover align-middle mb-0"
        >
          <thead>
            <tr>
              <th>Name</th>
              <th>Phone</th>
              <th>Active</th>
              <th>Current Requests</th>
              <th class="text-end">
                Actions
              </th>
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
            <tr v-else-if="!filtered.length">
              <td
                colspan="5"
                class="text-center text-muted py-4"
              >
                No technicians found.
              </td>
            </tr>
            <tr
              v-for="t in filtered"
              :key="t.id"
            >
              <td class="fw-semibold">
                {{ t.fullName }}
              </td>
              <td>{{ t.phone }}</td>
              <td>
                <span
                  class="badge"
                  :class="t.active ? 'text-bg-success' : 'text-bg-secondary'"
                >{{ t.active ? 'Active' : 'Inactive' }}</span>
                <div class="form-check form-switch d-inline-block ms-2 align-middle">
                  <input
                    class="form-check-input"
                    type="checkbox"
                    role="switch"
                    :checked="!!t.active"
                    :aria-label="`Toggle ${t.fullName}`"
                    @change="toggleActive(t)"
                  >
                </div>
              </td>
              <td>{{ t.currentRequests ?? 0 }}</td>
              <td class="text-end text-nowrap">
                <button
                  type="button"
                  class="btn btn-sm btn-outline-secondary"
                  title="Assigned requests"
                  @click="openAssigned(t)"
                >
                  <i class="bi bi-clipboard-check" />
                </button>
                <button
                  type="button"
                  class="btn btn-sm btn-outline-secondary ms-1"
                  title="Edit"
                  @click="openEdit(t)"
                >
                  <i class="bi bi-pencil" />
                </button>
                <button
                  type="button"
                  class="btn btn-sm btn-outline-danger ms-1"
                  title="Delete"
                  @click="removeTechnician(t)"
                >
                  <i class="bi bi-trash" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <AppModal
      id="technician-modal"
      :title="editingId == null ? 'New Technician' : 'Edit Technician'"
    >
      <form @submit.prevent="save">
        <div class="mb-3">
          <label
            for="technician-name"
            class="form-label"
          >Full name</label>
          <input
            id="technician-name"
            v-model="form.fullName"
            type="text"
            class="form-control"
            required
          >
        </div>
        <div class="mb-3">
          <label
            for="technician-phone"
            class="form-label"
          >Phone</label>
          <input
            id="technician-phone"
            v-model="form.phone"
            type="tel"
            class="form-control"
            placeholder="+998..."
            required
          >
        </div>
      </form>
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
          :disabled="saving"
          @click="save"
        >
          {{ saving ? 'Saving...' : 'Save' }}
        </button>
      </template>
    </AppModal>

    <AppModal
      id="assigned-modal"
      size="lg"
      :title="`Assigned Requests — ${data?.find(t => t.id === viewingId)?.fullName || ''}`"
    >
      <div
        v-if="loadingAssigned"
        class="text-center py-4"
      >
        <div class="spinner-border spinner-border-sm text-primary" />
      </div>
      <div
        v-else-if="assignedError"
        class="alert alert-danger mb-0"
      >
        {{ assignedError }}
      </div>
      <table
        v-else
        class="table table-striped align-middle mb-0"
      >
        <thead>
          <tr>
            <th>#</th>
            <th>Category</th>
            <th>Status</th>
            <th>Date</th>
            <th class="text-end">
              Actions
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="!assigned.length">
            <td
              colspan="5"
              class="text-center text-muted py-4"
            >
              No requests assigned.
            </td>
          </tr>
          <tr
            v-for="r in assigned"
            :key="r.id"
          >
            <td>
              <NuxtLink :to="`/requests/${r.id}`">
                #{{ r.id }}
              </NuxtLink>
            </td>
            <td>{{ categoryName(r) }}</td>
            <td><StatusBadge :status="r.status" /></td>
            <td>{{ formatDate(r.createdAt) }}</td>
            <td class="text-end">
              <NuxtLink
                :to="`/requests/${r.id}`"
                class="btn btn-sm btn-outline-secondary"
              >
                <i class="bi bi-eye" />
              </NuxtLink>
            </td>
          </tr>
        </tbody>
      </table>
    </AppModal>
  </AppContent>
</template>
