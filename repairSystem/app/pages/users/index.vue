<script setup lang="ts">
import type { CrmUser, Page, UserRole } from '~/types'
import { getApiErrorMessage } from '~/utils/api'

const { t } = useLocale()
const { user: currentUser } = useAuth()
const isAdmin = computed(() => currentUser.value?.role === 'ADMIN')

const page = ref(1)
const size = ref(20)
const roleFilter = ref('all')

const query = computed(() => ({
  page: page.value - 1,
  size: size.value,
  role: roleFilter.value === 'all' ? undefined : roleFilter.value
}))

const { data, pending, error, refresh } = await useAsyncData('users-list', () =>
  apiFetch<Page<CrmUser>>('/users', { query: query.value }),
{ immediate: isAdmin.value }
)

const errorMessage = computed(() => {
  const err = error.value as { data?: { message?: string } } | null
  return err?.data?.message || error.value?.message || t('failedToLoadUsers')
})

const rows = computed(() => data.value?.content ?? [])
const totalElements = computed(() => data.value?.totalElements ?? 0)
const totalPages = computed(() => data.value?.totalPages ?? 1)

function setRoleFilter(role: string) {
  roleFilter.value = role
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
const form = ref({ fullName: '', email: '', password: '', role: 'MANAGER' as UserRole })
const originalRole = ref<UserRole>('MANAGER')
const saving = ref(false)
const saveError = ref('')

function openCreate() {
  editingId.value = null
  form.value = { fullName: '', email: '', password: '', role: 'MANAGER' }
  saveError.value = ''
  showModal('user-modal')
}

function openEdit(u: CrmUser) {
  editingId.value = u.id
  form.value = { fullName: u.fullName, email: u.email, password: '', role: u.role }
  originalRole.value = u.role
  saveError.value = ''
  showModal('user-modal')
}

async function save() {
  if (editingId.value == null && !form.value.password) return
  saving.value = true
  saveError.value = ''
  try {
    if (editingId.value == null) {
      await apiFetch('/users', {
        method: 'POST',
        body: {
          fullName: form.value.fullName,
          email: form.value.email,
          password: form.value.password,
          role: form.value.role
        }
      })
    } else {
      await apiFetch(`/users/${editingId.value}`, {
        method: 'PUT',
        body: { fullName: form.value.fullName, email: form.value.email }
      })
      if (form.value.role !== originalRole.value) {
        await apiFetch(`/users/${editingId.value}/role`, {
          method: 'PATCH',
          body: { role: form.value.role }
        })
      }
    }
    hideModal('user-modal')
    await refresh()
  } catch (e) {
    saveError.value = getApiErrorMessage(e, 'Failed to save user.')
  } finally {
    saving.value = false
  }
}

const togglingId = ref<number | null>(null)
const toggleError = ref('')

async function toggleActive(u: CrmUser) {
  togglingId.value = u.id
  toggleError.value = ''
  try {
    const details = await apiFetch<{ active: boolean }>(`/users/${u.id}`)
    await apiFetch(`/users/${u.id}/activation`, {
      method: 'PATCH',
      body: { active: !details.active }
    })
    await refresh()
  } catch (e) {
    toggleError.value = getApiErrorMessage(e, 'Failed to change activation.')
  } finally {
    togglingId.value = null
  }
}
</script>

<template>
  <AppContent
    :title="t('users')"
    :breadcrumbs="[{ label: t('home'), to: '/' }, { label: t('users') }]"
  >
    <div
      v-if="!isAdmin"
      class="alert alert-warning"
    >
      {{ t('notAuthorized') }}
    </div>

    <div
      v-else
      class="card"
    >
      <div class="card-header">
        <div class="d-flex flex-column flex-md-row gap-2 align-items-md-center justify-content-between">
          <div
            class="btn-group"
            role="group"
            aria-label="Filter users"
          >
            <button
              type="button"
              class="btn"
              :class="roleFilter === 'all' ? 'btn-primary' : 'btn-outline-secondary'"
              @click="setRoleFilter('all')"
            >
              {{ t('all') }}
            </button>
            <button
              type="button"
              class="btn"
              :class="roleFilter === 'ADMIN' ? 'btn-primary' : 'btn-outline-secondary'"
              @click="setRoleFilter('ADMIN')"
            >
              {{ t('admins') }}
            </button>
            <button
              type="button"
              class="btn"
              :class="roleFilter === 'MANAGER' ? 'btn-primary' : 'btn-outline-secondary'"
              @click="setRoleFilter('MANAGER')"
            >
              {{ t('managers') }}
            </button>
          </div>

          <button
            type="button"
            class="btn btn-sm btn-primary"
            @click="openCreate"
          >
            <i class="bi bi-plus-lg me-1" />{{ t('newUser') }}
          </button>
        </div>
      </div>

      <div
        v-if="toggleError"
        class="alert alert-danger m-3 py-2"
      >
        {{ toggleError }}
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
              <th>{{ t('fullName') }}</th>
              <th>{{ t('email') }}</th>
              <th>{{ t('role') }}</th>
              <th class="text-end">
                {{ t('actions') }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="pending">
              <td
                colspan="4"
                class="text-center py-4"
              >
                <div class="spinner-border spinner-border-sm text-primary" />
              </td>
            </tr>

            <tr v-else-if="!rows.length">
              <td
                colspan="4"
                class="text-center py-4"
              >
                <div class="empty-state">
                  <i class="bi bi-people" />
                  <p>{{ t('noUsersFound') }}</p>
                </div>
              </td>
            </tr>

            <tr
              v-for="u in rows"
              v-else
              :key="u.id"
            >
              <td class="fw-semibold">
                {{ u.fullName }}
              </td>
              <td>{{ u.email }}</td>
              <td>
                <span
                  class="badge"
                  :class="u.role === 'ADMIN' ? 'text-bg-danger' : 'text-bg-secondary'"
                >
                  {{ u.role === 'ADMIN' ? t('admin') : t('manager') }}
                </span>
              </td>
              <td class="text-end text-nowrap">
                <button
                  type="button"
                  class="btn btn-sm btn-outline-secondary"
                  :title="t('edit')"
                  @click="openEdit(u)"
                >
                  <i class="bi bi-pencil" />
                </button>
                <button
                  type="button"
                  class="btn btn-sm btn-outline-secondary ms-1"
                  :title="t('active')"
                  :disabled="togglingId === u.id"
                  @click="toggleActive(u)"
                >
                  <i class="bi bi-toggle-on" />
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
        @update:page="goToPage"
        @update:size="changeSize"
      />
    </div>

    <AppModal
      id="user-modal"
      :title="editingId == null ? t('newUser') : t('editUser')"
    >
      <form @submit.prevent="save">
        <div class="mb-3">
          <label
            for="user-name"
            class="form-label"
          >{{ t('fullName') }}</label>
          <input
            id="user-name"
            v-model="form.fullName"
            type="text"
            class="form-control"
            required
          >
        </div>

        <div class="mb-3">
          <label
            for="user-email"
            class="form-label"
          >{{ t('email') }}</label>
          <input
            id="user-email"
            v-model="form.email"
            type="email"
            class="form-control"
            required
          >
        </div>

        <div class="mb-3">
          <label
            for="user-password"
            class="form-label"
          >{{ t('password') }}</label>
          <input
            id="user-password"
            v-model="form.password"
            type="password"
            class="form-control"
            autocomplete="new-password"
            :required="editingId == null"
          >
        </div>

        <div class="mb-3">
          <label
            for="user-role"
            class="form-label"
          >{{ t('role') }}</label>
          <select
            id="user-role"
            v-model="form.role"
            class="form-select"
          >
            <option value="ADMIN">
              {{ t('admin') }}
            </option>
            <option value="MANAGER">
              {{ t('manager') }}
            </option>
          </select>
        </div>

        <div
          v-if="saveError"
          class="alert alert-danger py-2"
        >
          {{ saveError }}
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
  </AppContent>
</template>
