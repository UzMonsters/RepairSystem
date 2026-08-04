<script setup lang="ts">
import type { CrmUser, UserRole } from '~/types'

const { t } = useLocale()
const { data, pending, error, refresh } = await useAsyncData('users-list', () =>
  apiFetch<CrmUser[]>('/users')
)

const filterTab = ref<'all' | 'ADMIN' | 'MANAGER'>('all')
const roles: UserRole[] = ['ADMIN', 'MANAGER']

const filteredUsers = computed(() => {
  if (filterTab.value === 'all') return data.value || []
  return (data.value || []).filter(user => user.role === filterTab.value)
})

const errorMessage = computed(() => {
  const err = error.value as { data?: { message?: string } } | null
  return err?.data?.message || error.value?.message || t('failedToLoadUsers')
})

const editingId = ref<number | null>(null)
const form = ref({ fullName: '', email: '', password: '', role: 'MANAGER' as UserRole })
const saving = ref(false)

function openCreate() {
  editingId.value = null
  form.value = { fullName: '', email: '', password: '', role: 'MANAGER' }
  showModal('user-modal')
}

function openEdit(u: CrmUser) {
  editingId.value = u.id
  form.value = { fullName: u.fullName, email: u.email, password: '', role: u.role }
  showModal('user-modal')
}

async function save() {
  if (editingId.value == null && !form.value.password) return
  saving.value = true
  try {
    const body: Record<string, string> = {
      fullName: form.value.fullName,
      email: form.value.email,
      role: form.value.role
    }
    if (form.value.password) body.password = form.value.password
    if (editingId.value == null) {
      await apiFetch('/users', { method: 'POST', body })
    } else {
      await apiFetch(`/users/${editingId.value}`, { method: 'PATCH', body })
    }
    hideModal('user-modal')
    refresh()
  } catch (e) {
    void e
  } finally {
    saving.value = false
  }
}

async function removeUser(u: CrmUser) {
  if (!confirm(`Delete user "${u.fullName}"? This action cannot be undone.`)) return
  try {
    await apiFetch(`/users/${u.id}`, { method: 'DELETE' })
    refresh()
  } catch (e) {
    void e
  }
}
</script>

<template>
  <AppContent
    :title="t('users')"
    :breadcrumbs="[{ label: t('home'), to: '/' }, { label: t('users') }]"
  >
    <div class="card mb-4">
      <div class="card-header d-flex align-items-center justify-content-between">
        <div>
          <div class="btn-group" role="group" aria-label="Filter users">
            <button
              type="button"
              class="btn"
              :class="filterTab === 'all' ? 'btn-primary' : 'btn-outline-secondary'"
              @click="filterTab = 'all'"
            >
              {{ t('all') }}
            </button>
            <button
              type="button"
              class="btn"
              :class="filterTab === 'ADMIN' ? 'btn-primary' : 'btn-outline-secondary'"
              @click="filterTab = 'ADMIN'"
            >
              {{ t('admins') }}
            </button>
            <button
              type="button"
              class="btn"
              :class="filterTab === 'MANAGER' ? 'btn-primary' : 'btn-outline-secondary'"
              @click="filterTab = 'MANAGER'"
            >
              {{ t('managers') }}
            </button>
          </div>
        </div>

        <button type="button" class="btn btn-sm btn-primary" @click="openCreate">
          <i class="bi bi-plus-lg me-1" />{{ t('newUser') }}
        </button>
      </div>

      <div class="card-body table-responsive p-0">
        <div v-if="error" class="alert alert-danger m-3">
          {{ errorMessage }}
          <button type="button" class="btn btn-sm btn-outline-danger ms-2" @click="refresh">{{ t('retry') || 'Retry' }}</button>
        </div>

        <table v-else class="table table-striped table-hover align-middle mb-0">
          <thead>
            <tr>
              <th>{{ t('fullName') }}</th>
              <th>{{ t('email') }}</th>
              <th>{{ t('role') }}</th>
              <th class="text-end">{{ t('actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="pending">
              <td colspan="4" class="text-center py-4">
                <div class="spinner-border spinner-border-sm text-primary" />
              </td>
            </tr>

            <tr v-else-if="!filteredUsers.length">
              <td colspan="4" class="text-center text-muted py-4">{{ t('noUsersFound') }}</td>
            </tr>

            <tr v-else v-for="u in filteredUsers" :key="u.id">
              <td class="fw-semibold">{{ u.fullName }}</td>
              <td>{{ u.email }}</td>
              <td>
                <span class="badge" :class="u.role === 'ADMIN' ? 'text-bg-danger' : 'text-bg-secondary'">{{ u.role }}</span>
              </td>
              <td class="text-end text-nowrap">
                <button type="button" class="btn btn-sm btn-outline-secondary" :title="t('edit')" @click="openEdit(u)">
                  <i class="bi bi-pencil" />
                </button>
                <button type="button" class="btn btn-sm btn-outline-danger ms-1" :title="t('delete')" @click="removeUser(u)">
                  <i class="bi bi-trash" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <AppModal id="user-modal" :title="editingId == null ? t('newUser') : t('editUser')">
      <template #default>
        <div class="mb-3">
          <label for="user-name" class="form-label">{{ t('fullName') }}</label>
          <input id="user-name" v-model="form.fullName" type="text" class="form-control" :placeholder="t('fullName')" required />
        </div>

        <div class="mb-3">
          <label for="user-email" class="form-label">{{ t('email') }}</label>
          <input id="user-email" v-model="form.email" type="email" class="form-control" :placeholder="t('email')" required />
        </div>

        <div class="mb-3">
          <label for="user-password" class="form-label">{{ t('password') }}</label>
          <input id="user-password" v-model="form.password" type="password" class="form-control" autocomplete="new-password" :placeholder="editingId == null ? t('required') : t('leaveBlank')" :required="editingId == null" />
        </div>

        <div class="mb-3">
          <label for="user-role" class="form-label">Role</label>
          <select id="user-role" v-model="form.role" class="form-select">
            <option v-for="r in roles" :key="r" :value="r">{{ r }}</option>
          </select>
        </div>
      </template>

      <template #footer>
        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">{{ t('cancel') }}</button>
        <button type="button" class="btn btn-primary" :disabled="saving" @click="save">{{ saving ? t('saving') : t('save') }}</button>
      </template>
    </AppModal>
  </AppContent>
</template>
