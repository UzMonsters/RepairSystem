<script setup lang="ts">
import type { CrmUser, UserRole } from '~/types'

const { data, pending, error, refresh } = await useAsyncData('users-list', () =>
  apiFetch<CrmUser[]>('/users')
)

const errorMessage = computed(() => {
  const err = error.value as { data?: { message?: string } } | null
  return err?.data?.message || error.value?.message || 'Failed to load users.'
})

const roles: UserRole[] = ['ADMIN', 'MANAGER']

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
    title="Users"
    :breadcrumbs="[{ label: 'Home', to: '/' }, { label: 'Users' }]"
  >
    <div class="card">
      <div class="card-header">
        <div class="d-flex flex-column flex-md-row gap-2 align-items-md-center justify-content-between">
          <h3 class="card-title mb-0">
            Administrators &amp; Managers
          </h3>
          <button
            type="button"
            class="btn btn-sm btn-primary"
            @click="openCreate"
          >
            <i class="bi bi-plus-lg me-1" />New User
          </button>
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
              <th>Full name</th>
              <th>Email</th>
              <th>Role</th>
              <th class="text-end">
                Actions
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
            <tr v-else-if="!data?.length">
              <td
                colspan="4"
                class="text-center text-muted py-4"
              >
                No users found.
              </td>
            </tr>
            <tr
              v-for="u in data"
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
                >{{ u.role }}</span>
              </td>
              <td class="text-end text-nowrap">
                <button
                  type="button"
                  class="btn btn-sm btn-outline-secondary"
                  title="Edit"
                  @click="openEdit(u)"
                >
                  <i class="bi bi-pencil" />
                </button>
                <button
                  type="button"
                  class="btn btn-sm btn-outline-danger ms-1"
                  title="Delete"
                  @click="removeUser(u)"
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
      id="user-modal"
      :title="editingId == null ? 'New User' : 'Edit User'"
    >
      <form @submit.prevent="save">
        <div class="mb-3">
          <label
            for="user-name"
            class="form-label"
          >Full name</label>
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
          >Email</label>
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
          >Password</label>
          <input
            id="user-password"
            v-model="form.password"
            type="password"
            class="form-control"
            autocomplete="new-password"
            :placeholder="editingId == null ? 'Required' : 'Leave blank to keep current'"
            :required="editingId == null"
          >
        </div>
        <div class="mb-3">
          <label
            for="user-role"
            class="form-label"
          >Role</label>
          <select
            id="user-role"
            v-model="form.role"
            class="form-select"
          >
            <option
              v-for="r in roles"
              :key="r"
              :value="r"
            >
              {{ r }}
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
  </AppContent>
</template>
