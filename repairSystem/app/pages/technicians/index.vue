<script setup lang="ts">
import type { LanguageCode, Page, Technician } from '~/types'

const { t } = useLocale()
const search = ref('')
const page = ref(1)
const size = ref(10)

const query = computed(() => ({
  page: page.value - 1,
  size: size.value,
  search: search.value.trim() || undefined
}))

const { data, pending, error, refresh } = await useAsyncData('technicians-list', () =>
  apiFetch<Page<Technician>>('/technicians', { query: query.value })
)

const errorMessage = computed(() => {
  const err = error.value as { data?: { message?: string } } | null
  return err?.data?.message || error.value?.message || 'Failed to load technicians.'
})

const rows = computed(() => data.value?.content ?? [])
const totalElements = computed(() => data.value?.totalElements ?? 0)
const totalPages = computed(() => data.value?.totalPages ?? 1)

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

const editingId = ref<number | null>(null)
const form = ref({
  fullName: '',
  phone: '',
  specialization: '',
  notes: '',
  maximumConcurrentRequests: 5,
  preferredLanguage: 'UZ' as LanguageCode
})
const saving = ref(false)
const saveError = ref('')

function openCreate() {
  editingId.value = null
  form.value = { fullName: '', phone: '', specialization: '', notes: '', maximumConcurrentRequests: 5, preferredLanguage: 'UZ' }
  saveError.value = ''
  showModal('technician-modal')
}

function openEdit(tech: Technician) {
  editingId.value = tech.id
  form.value = {
    fullName: tech.fullName,
    phone: tech.phone,
    specialization: tech.specialization ?? '',
    notes: '',
    maximumConcurrentRequests: tech.maximumConcurrentRequests ?? 5,
    preferredLanguage: tech.preferredLanguage ?? 'UZ'
  }
  saveError.value = ''
  showModal('technician-modal')
}

async function save() {
  saving.value = true
  saveError.value = ''
  try {
    const body = {
      fullName: form.value.fullName,
      phone: form.value.phone,
      specialization: form.value.specialization || undefined,
      notes: form.value.notes || undefined,
      maximumConcurrentRequests: form.value.maximumConcurrentRequests,
      preferredLanguage: form.value.preferredLanguage
    }
    if (editingId.value == null) {
      await apiFetch('/technicians', { method: 'POST', body })
    } else {
      await apiFetch(`/technicians/${editingId.value}`, { method: 'PUT', body })
    }
    hideModal('technician-modal')
    refresh()
  } catch (e) {
    const err = e as { data?: { message?: string }, message?: string }
    saveError.value = err.data?.message || err.message || 'Failed to save technician.'
  } finally {
    saving.value = false
  }
}

const togglingId = ref<number | null>(null)

async function toggleActive(tech: Technician) {
  togglingId.value = tech.id
  try {
    await apiFetch(`/technicians/${tech.id}/activation`, { method: 'PATCH', body: { active: !tech.active } })
    refresh()
  } catch (e) {
    void e
  } finally {
    togglingId.value = null
  }
}

const viewingId = ref<number | null>(null)
const workload = ref<Record<string, unknown> | null>(null)
const loadingWorkload = ref(false)
const workloadError = ref('')

async function openWorkload(tech: Technician) {
  viewingId.value = tech.id
  workload.value = null
  workloadError.value = ''
  showModal('workload-modal')
  loadingWorkload.value = true
  try {
    workload.value = await apiFetch<Record<string, unknown>>(`/technicians/${tech.id}/workload`)
  } catch (e) {
    const err = e as { data?: { message?: string }, message?: string }
    workloadError.value = err.data?.message || err.message || 'Failed to load workload.'
  } finally {
    loadingWorkload.value = false
  }
}

function formatDate(value?: string) {
  return value ? new Date(value).toLocaleDateString() : '-'
}
</script>

<template>
  <AppContent
    :title="t('technicians')"
    :breadcrumbs="[{ label: t('home'), to: '/' }, { label: t('technicians') }]"
  >
    <div class="card">
      <div class="card-header">
        <div class="d-flex flex-column flex-md-row gap-2 align-items-md-center justify-content-between">
          <h3 class="card-title mb-0">{{ t('technicians') }}</h3>
          <div class="d-flex gap-2 flex-wrap">
            <div class="input-group input-group-sm search-box">
              <input
                v-model="search"
                type="search"
                class="form-control"
                :placeholder="t('searchByNameOrPhone')"
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
            <button type="button" class="btn btn-sm btn-primary" @click="openCreate">
              <i class="bi bi-plus-lg me-1" />{{ t('newTechnician') }}
            </button>
          </div>
        </div>
      </div>

      <div class="card-body table-responsive p-0">
        <div v-if="error" class="alert alert-danger m-3">
          {{ errorMessage }}
          <button type="button" class="btn btn-sm btn-outline-danger ms-2" @click="refresh">{{ t('retry') }}</button>
        </div>

        <table
          v-else
          class="table table-hover align-middle mb-0"
        >
          <thead>
            <tr>
              <th>{{ t('fullName') }}</th>
              <th>{{ t('phone') }}</th>
              <th>{{ t('specialization') }}</th>
              <th>{{ t('maxConcurrentRequests') }}</th>
              <th>{{ t('language') }}</th>
              <th>{{ t('active') }}</th>
              <th>{{ t('created') }}</th>
              <th class="text-end">{{ t('actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="pending">
              <td colspan="8" class="text-center py-4">
                <div class="spinner-border spinner-border-sm text-primary" />
              </td>
            </tr>
            <tr v-else-if="!rows.length">
              <td colspan="8" class="text-center py-4">
                <div class="empty-state">
                  <i class="bi bi-person-wrench" />
                  <p>{{ t('noTechniciansFound') }}</p>
                </div>
              </td>
            </tr>
            <tr v-for="tech in rows" :key="tech.id">
              <td class="fw-semibold">{{ tech.fullName }}</td>
              <td>{{ tech.phone }}</td>
              <td>{{ tech.specialization || '-' }}</td>
              <td>{{ tech.maximumConcurrentRequests ?? '-' }}</td>
              <td>{{ tech.preferredLanguage ? t(`language.${tech.preferredLanguage}`) : '-' }}</td>
              <td>
                <span class="form-check form-switch d-inline-block align-middle mb-0">
                  <input
                    class="form-check-input"
                    type="checkbox"
                    role="switch"
                    :checked="!!tech.active"
                    :disabled="togglingId === tech.id"
                    :aria-label="`Toggle ${tech.fullName}`"
                    @change="toggleActive(tech)"
                  />
                </span>
                <span class="badge ms-1" :class="tech.active ? 'text-bg-success' : 'text-bg-secondary'">{{ t(tech.active ? 'active' : 'inactive') }}</span>
              </td>
              <td class="text-nowrap">{{ formatDate(tech.createdAt) }}</td>
              <td class="text-end text-nowrap">
                <button type="button" class="btn btn-sm btn-outline-secondary" :title="t('workload')" @click="openWorkload(tech)">
                  <i class="bi bi-clipboard-check" />
                </button>
                <button type="button" class="btn btn-sm btn-outline-secondary ms-1" :title="t('edit')" @click="openEdit(tech)">
                  <i class="bi bi-pencil" />
                </button>
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
        :page-sizes="[10, 25, 50, 100]"
        @update:page="goToPage"
        @update:size="changeSize"
      />
    </div>

    <AppModal id="technician-modal" :title="editingId == null ? t('newTechnician') : t('edit')">
      <form @submit.prevent="save">
        <div class="mb-3">
          <label for="technician-name" class="form-label">{{ t('fullName') }}</label>
          <input id="technician-name" v-model="form.fullName" type="text" class="form-control" required />
        </div>
        <div class="mb-3">
          <label for="technician-phone" class="form-label">{{ t('phone') }}</label>
          <input id="technician-phone" v-model="form.phone" type="tel" class="form-control" placeholder="+998..." required />
        </div>
        <div class="mb-3">
          <label for="technician-spec" class="form-label">{{ t('specialization') }}</label>
          <input id="technician-spec" v-model="form.specialization" type="text" class="form-control" />
        </div>
        <div class="mb-3">
          <label for="technician-max" class="form-label">{{ t('maxConcurrentRequests') }}</label>
          <input id="technician-max" v-model.number="form.maximumConcurrentRequests" type="number" min="1" max="100" class="form-control" />
        </div>
        <div class="mb-3">
          <label for="technician-lang" class="form-label">{{ t('language') }}</label>
          <select id="technician-lang" v-model="form.preferredLanguage" class="form-select">
            <option value="UZ">UZ</option>
            <option value="RU">RU</option>
            <option value="EN">EN</option>
          </select>
        </div>
        <div class="mb-3">
          <label for="technician-notes" class="form-label">{{ t('description') }}</label>
          <textarea id="technician-notes" v-model="form.notes" class="form-control" rows="2" />
        </div>
        <div v-if="saveError" class="alert alert-danger py-2">{{ saveError }}</div>
      </form>
      <template #footer>
        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">{{ t('cancel') }}</button>
        <button type="button" class="btn btn-primary" :disabled="saving" @click="save">
          {{ saving ? t('saving') : t('save') }}
        </button>
      </template>
    </AppModal>

    <AppModal id="workload-modal" :title="`${t('workload')} — ${rows.find(x => x.id === viewingId)?.fullName || ''}`">
      <div v-if="loadingWorkload" class="text-center py-4">
        <div class="spinner-border spinner-border-sm text-primary" />
      </div>
      <div v-else-if="workloadError" class="alert alert-danger mb-0">{{ workloadError }}</div>
      <div v-else-if="workload" class="workload-grid">
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
    </AppModal>
  </AppContent>
</template>
