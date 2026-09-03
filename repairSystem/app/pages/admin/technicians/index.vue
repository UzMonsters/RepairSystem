<script setup lang="ts">
import type { LanguageCode, Page, Technician } from '~/types'
import { apiAssetUrl, getApiErrorCode, getApiErrorMessage } from '~/utils/api'
import { formatDate as formatApiDate } from '~/utils/date'

const { t } = useLocale()
const search = ref('')
const page = ref(1)
const size = ref(10)
const sortField = ref('createdAt')
const sortDirection = ref<'asc' | 'desc'>('desc')

const query = computed(() => ({
  page: page.value - 1,
  size: size.value,
  search: search.value.trim() || undefined,
  sort: `${sortField.value},${sortDirection.value}`
}))

const { data, pending, error, refresh } = await useAsyncData('technicians-list', () =>
  apiFetch<Page<Technician>>('/technicians', { query: query.value })
)

const errorMessage = computed(() => {
  return getApiErrorMessage(error.value, 'Failed to load technicians.')
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
  email: '',
  specialization: '',
  notes: '',
  maximumConcurrentRequests: 5,
  preferredLanguage: 'UZ' as LanguageCode
})
const saving = ref(false)
const saveError = ref('')

const phoneInvalid = ref(false)
const maxInvalid = ref(false)

function resetFormErrors() {
  saveError.value = ''
  phoneInvalid.value = false
  maxInvalid.value = false
}

function isValidPhone(value: string) {
  let digits = value.trim().replace(/[\s()-]/g, '')
  if (digits.startsWith('+')) digits = digits.slice(1)
  return (digits.startsWith('998') && digits.length === 12) || digits.length === 9
}

function openCreate() {
  editingId.value = null
  form.value = { fullName: '', phone: '', email: '', specialization: '', notes: '', maximumConcurrentRequests: 5, preferredLanguage: 'UZ' }
  resetFormErrors()
  showModal('technician-modal')
}

function openMessage(tech: Technician) {
  useToast().showInfo('Функция отправки сообщений (Message) будет доступна в следующем обновлении.')
}

function openEdit(tech: Technician) {
  editingId.value = tech.id
  form.value = {
    fullName: tech.fullName,
    phone: tech.phone,
    email: tech.email ?? '',
    specialization: tech.specialization ?? '',
    notes: '',
    maximumConcurrentRequests: tech.maximumConcurrentRequests ?? 5,
    preferredLanguage: tech.preferredLanguage ?? 'UZ'
  }
  resetFormErrors()
  showModal('technician-modal')
}

async function save() {
  saving.value = true
  resetFormErrors()
  try {
    const phoneValue = form.value.phone.trim()
    if (!isValidPhone(phoneValue)) {
      phoneInvalid.value = true
      return
    }
    const max = form.value.maximumConcurrentRequests
    if (typeof max === 'number' && (max < 1 || max > 100)) {
      maxInvalid.value = true
      return
    }
    const body = {
      fullName: form.value.fullName.trim(),
      phone: phoneValue,
      email: form.value.email.trim() || undefined,
      specialization: form.value.specialization || undefined,
      notes: form.value.notes || undefined,
      maximumConcurrentRequests: typeof max === 'number' ? max : undefined,
      preferredLanguage: form.value.preferredLanguage
    }
    if (editingId.value == null) {
      await apiFetch('/technicians', { method: 'POST', body })
    } else {
      await apiFetch(`/technicians/${editingId.value}`, { method: 'PUT', body })
    }
    hideModal('technician-modal')
    await refresh()
  } catch (e) {
    saveError.value = getApiErrorCode(e) === 'INVALID_PHONE_NUMBER'
      ? t('invalidPhoneNumber')
      : getApiErrorMessage(e, 'Failed to save technician.')
  } finally {
    saving.value = false
  }
}

const togglingId = ref<number | null>(null)

async function toggleActive(tech: Technician) {
  togglingId.value = tech.id
  try {
    await apiFetch(`/technicians/${tech.id}/activation`, { method: 'PATCH', body: { active: !tech.active } })
    await refresh()
  } catch (e) {
    saveError.value = getApiErrorMessage(e, 'Failed to change technician status.')
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
    workloadError.value = getApiErrorMessage(e, 'Failed to load workload.')
  } finally {
    loadingWorkload.value = false
  }
}

const telegramTech = ref<Technician | null>(null)
const telegramDeepLink = ref('')
const telegramExpiresAt = ref('')
const generatingLink = ref(false)
const telegramLinkError = ref('')

async function openTelegramLink(tech: Technician) {
  telegramTech.value = tech
  telegramDeepLink.value = ''
  telegramExpiresAt.value = ''
  telegramLinkError.value = ''
  showModal('telegram-link-modal')
  generatingLink.value = true
  try {
    const res = await apiFetch<{ deepLink: string, expiresAt?: string }>(`/technicians/${tech.id}/telegram-link`, { method: 'POST' })
    telegramDeepLink.value = res.deepLink
    telegramExpiresAt.value = res.expiresAt ?? ''
  } catch (e) {
    telegramLinkError.value = getApiErrorMessage(e, 'Failed to create Telegram link.')
  } finally {
    generatingLink.value = false
  }
}

const copied = ref(false)

async function copyTelegramLink() {
  if (!telegramDeepLink.value) return
  try {
    await navigator.clipboard.writeText(telegramDeepLink.value)
    copied.value = true
    setTimeout(() => (copied.value = false), 2000)
  } catch {
    void telegramDeepLink.value
  }
}

function formatDate(value?: string) {
  return formatApiDate(value)
}

function toggleSort(field: string) {
  if (sortField.value === field) sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
  else {
    sortField.value = field
    sortDirection.value = 'asc'
  }
  page.value = 1
  refresh()
}

function sortIcon(field: string) {
  return sortField.value === field
    ? sortDirection.value === 'asc' ? 'bi-arrow-up' : 'bi-arrow-down'
    : 'bi-arrow-down-up'
}

function openTechnician(id: number) {
  navigateTo(`/admin/technicians/${id}`)
}
</script>

<template>
  <AppContent
    :title="t('technicians')"
    :breadcrumbs="[{ label: t('home'), to: '/admin' }, { label: t('technicians') }]"
  >
    <div class="card">
      <div class="card-header">
        <div class="d-flex flex-column flex-md-row gap-2 align-items-md-center justify-content-between">
          <h3 class="card-title mb-0">
            {{ t('technicians') }}
          </h3>
          <div class="d-flex gap-2 flex-nowrap">
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
            <button
              type="button"
              class="btn btn-sm btn-primary  w-75"
              @click="openCreate"
            >
              <i class="bi bi-plus-lg me-1" />{{ t('newTechnician') }}
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
            @click="() => { refresh() }"
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
                  { label: t('fullName'), field: 'fullName' },
                  { label: t('phone'), field: 'phone' },
                  { label: t('email'), field: 'email' },
                  { label: t('specialization'), field: 'specialization' },
                  { label: t('maxConcurrentRequests'), field: 'maximumConcurrentRequests' },
                  { label: t('language'), field: 'preferredLanguage' },
                  { label: t('active'), field: 'active' },
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
                colspan="9"
                class="text-center py-4"
              >
                <div class="spinner-border spinner-border-sm text-primary" />
              </td>
            </tr>
            <tr v-else-if="!rows.length">
              <td
                colspan="9"
                class="text-center py-4"
              >
                <div class="empty-state">
                  <i class="bi bi-person-wrench" />
                  <p>{{ t('noTechniciansFound') }}</p>
                </div>
              </td>
            </tr>
            <tr
              v-for="tech in rows"
              :key="tech.id"
              class="table-row-link"
              tabindex="0"
              @click="openTechnician(tech.id)"
              @keydown.enter="openTechnician(tech.id)"
            >
              <td class="fw-semibold">
                <img
                  v-if="apiAssetUrl(tech.avatar?.downloadUrl)"
                  :src="apiAssetUrl(tech.avatar?.downloadUrl)"
                  :alt="tech.fullName"
                  class="entity-avatar me-2"
                >
                <span>
                  {{ tech.fullName }}
                </span>
              </td>
              <td>{{ tech.phone }}</td>
              <td>{{ tech.email || '-' }}</td>
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
                    @click.stop
                    @change="toggleActive(tech)"
                  >
                </span>
              </td>
              <td class="text-nowrap">
                {{ formatDate(tech.createdAt) }}
              </td>
              <td class="text-end text-nowrap">
                <div class="dropdown">
                  <button
                    class="btn btn-sm btn-outline-secondary"
                    type="button"
                    data-bs-toggle="dropdown"
                    @click.stop
                  >
                    <i class="bi bi-three-dots-vertical" />
                  </button>
                  <ul class="dropdown-menu dropdown-menu-end">
                    <li>
                      <NuxtLink
                        :to="`/admin/technicians/${tech.id}`"
                        class="dropdown-item"
                        @click.stop
                      >
                        <i class="bi bi-eye me-2" /> {{ t('view') }}
                      </NuxtLink>
                    </li>
                    <li>
                      <button
                        class="dropdown-item"
                        @click.stop="openEdit(tech)"
                      >
                        <i class="bi bi-pencil me-2" /> {{ t('edit') }}
                      </button>
                    </li>
                    <li>
                      <button
                        class="dropdown-item"
                        @click.stop="openTelegramLink(tech)"
                      >
                        <i class="bi bi-telegram me-2" /> {{ t('telegramLink') }}
                      </button>
                    </li>
                    <li>
                      <button
                        class="dropdown-item"
                        @click.stop="openWorkload(tech)"
                      >
                        <i class="bi bi-clipboard-check me-2" /> {{ t('workload') }}
                      </button>
                    </li>
                    <li><hr class="dropdown-divider"></li>
                    <li>
                      <button
                        class="dropdown-item"
                        @click.stop="openMessage(tech)"
                      >
                        <i class="bi bi-chat-dots me-2" /> Message
                      </button>
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
        :page-sizes="[10, 25, 50, 100]"
        @update:page="goToPage"
        @update:size="changeSize"
      />
    </div>

    <AppModal
      id="technician-modal"
      :title="editingId == null ? t('newTechnician') : t('edit')"
    >
      <form @submit.prevent="save">
        <div class="mb-3">
          <label
            for="technician-name"
            class="form-label"
          >{{ t('fullName') }}</label>
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
          >{{ t('phone') }}</label>
          <input
            id="technician-phone"
            v-model="form.phone"
            type="tel"
            class="form-control"
            :class="{ 'is-invalid': phoneInvalid }"
            placeholder="+998 XX XXX XX XX"
            required
            @input="phoneInvalid = false"
          >
          <div
            v-if="phoneInvalid"
            class="invalid-feedback d-block"
          >
            {{ t('invalidPhoneNumber') }}
          </div>
        </div>
        <div class="mb-3">
          <label
            for="technician-spec"
            class="form-label"
          >{{ t('specialization') }}</label>
          <input
            id="technician-spec"
            v-model="form.specialization"
            type="text"
            class="form-control"
          >
        </div>
        <div class="mb-3">
          <label
            for="technician-max"
            class="form-label"
          >{{ t('maxConcurrentRequests') }}</label>
          <input
            id="technician-max"
            v-model.number="form.maximumConcurrentRequests"
            type="number"
            min="1"
            max="100"
            class="form-control"
            :class="{ 'is-invalid': maxInvalid }"
            @input="maxInvalid = false"
          >
          <div
            v-if="maxInvalid"
            class="invalid-feedback d-block"
          >
            {{ t('invalidMaxConcurrentRequests') }}
          </div>
        </div>
        <div class="mb-3">
          <label
            for="technician-lang"
            class="form-label"
          >{{ t('language') }}</label>
          <select
            id="technician-lang"
            v-model="form.preferredLanguage"
            class="form-select"
          >
            <option value="UZ">
              UZ
            </option>
            <option value="RU">
              RU
            </option>
            <option value="EN">
              EN
            </option>
          </select>
        </div>
        <div class="mb-3">
          <label
            for="technician-notes"
            class="form-label"
          >{{ t('description') }}</label>
          <textarea
            id="technician-notes"
            v-model="form.notes"
            class="form-control"
            rows="2"
          />
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
          :disabled="saving"
          @click="save"
        >
          {{ saving ? t('saving') : t('save') }}
        </button>
      </template>
    </AppModal>

    <AppModal
      id="workload-modal"
      :title="`${t('workload')} — ${rows.find(x => x.id === viewingId)?.fullName || ''}`"
    >
      <div
        v-if="loadingWorkload"
        class="text-center py-4"
      >
        <div class="spinner-border spinner-border-sm text-primary" />
      </div>
      <div
        v-else-if="workloadError"
        class="alert alert-danger mb-0"
      >
        {{ workloadError }}
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
    </AppModal>

    <AppModal
      id="telegram-link-modal"
      :title="`${t('telegramLinkTitle')} — ${telegramTech?.fullName || ''}`"
    >
      <div
        v-if="generatingLink"
        class="text-center py-4"
      >
        <div class="spinner-border spinner-border-sm text-primary" />
      </div>
      <div
        v-else-if="telegramLinkError"
        class="alert alert-danger mb-0"
      >
        {{ telegramLinkError }}
      </div>
      <div v-else>
        <div
          v-if="telegramDeepLink"
          class="alert alert-info py-2"
        >
          <i class="bi bi-info-circle me-1" />
          {{ t('telegramLinkHint') }}
        </div>
        <div class="input-group mb-3">
          <input
            :value="telegramDeepLink"
            type="text"
            class="form-control"
            readonly
          >
          <button
            type="button"
            class="btn btn-outline-primary"
            :disabled="!telegramDeepLink"
            @click="copyTelegramLink"
          >
            <i class="bi bi-clipboard me-1" />{{ copied ? t('linkCopied') : t('copyLink') }}
          </button>
        </div>
        <div
          v-if="telegramExpiresAt"
          class="text-muted small"
        >
          {{ t('expiresAt') }}: {{ formatApiDate(telegramExpiresAt, true) }}
        </div>
      </div>
      <template #footer>
        <button
          type="button"
          class="btn btn-secondary"
          data-bs-dismiss="modal"
        >
          {{ t('close') }}
        </button>
      </template>
    </AppModal>
  </AppContent>
</template>
