<script setup lang="ts">
import type { Category } from '~/types'

const { data, pending, error, refresh } = await useAsyncData('categories-list', () =>
  apiFetch<Category[]>('/categories')
)

const errorMessage = computed(() => {
  const err = error.value as { data?: { message?: string } } | null
  return err?.data?.message || error.value?.message || 'Failed to load categories.'
})

const editingId = ref<number | null>(null)
const form = ref('')
const saving = ref(false)

function openCreate() {
  editingId.value = null
  form.value = ''
  showModal('category-modal')
}

function openEdit(c: Category) {
  editingId.value = c.id
  form.value = c.name
  showModal('category-modal')
}

async function save() {
  if (!form.value.trim()) return
  saving.value = true
  try {
    if (editingId.value == null) {
      await apiFetch('/categories', { method: 'POST', body: { name: form.value.trim() } })
    } else {
      await apiFetch(`/categories/${editingId.value}`, { method: 'PUT', body: { name: form.value.trim() } })
    }
    hideModal('category-modal')
    refresh()
  } catch (e) {
    void e
  } finally {
    saving.value = false
  }
}

async function removeCategory(c: Category) {
  if (!confirm(`Delete category "${c.name}"? This action cannot be undone.`)) return
  try {
    await apiFetch(`/categories/${c.id}`, { method: 'DELETE' })
    refresh()
  } catch (e) {
    void e
  }
}
</script>

<template>
  <AppContent
    title="Categories"
    :breadcrumbs="[{ label: 'Home', to: '/' }, { label: 'Categories' }]"
  >
    <div class="card">
      <div class="card-header">
        <div class="d-flex flex-column flex-md-row gap-2 align-items-md-center justify-content-between">
          <h3 class="card-title mb-0">
            Repair Categories
          </h3>
          <button
            type="button"
            class="btn btn-sm btn-primary"
            @click="openCreate"
          >
            <i class="bi bi-plus-lg me-1" />New Category
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
              <th>#</th>
              <th>Names</th>
              <th class="text-end">
                Actions
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="pending">
              <td
                colspan="3"
                class="text-center py-4"
              >
                <div class="spinner-border spinner-border-sm text-primary" />
              </td>
            </tr>
            <tr v-else-if="!data?.length">
              <td
                colspan="3"
                class="text-center text-muted py-4"
              >
                No categories found.
              </td>
            </tr>
            <tr
              v-for="c in data"
              :key="c.id"
            >
              <td>{{ c.id }}</td>
              <td class="fw-semibold">
                {{ c.name }}
              </td>
              <td class="text-end text-nowrap">
                <button
                  type="button"
                  class="btn btn-sm btn-outline-secondary"
                  title="Edit"
                  @click="openEdit(c)"
                >
                  <i class="bi bi-pencil" />
                </button>
                <button
                  type="button"
                  class="btn btn-sm btn-outline-danger ms-1"
                  title="Delete"
                  @click="removeCategory(c)"
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
      id="category-modal"
      :title="editingId == null ? 'New Category' : 'Edit Category'"
    >
      <form @submit.prevent="save">
        <div class="mb-3">
          <label
            for="category-name"
            class="form-label"
          >Name</label>
          <input
            id="category-name"
            v-model="form"
            type="text"
            class="form-control"
            placeholder="Air Conditioner"
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
          :disabled="saving || !form.trim()"
          @click="save"
        >
          {{ saving ? 'Saving...' : 'Save' }}
        </button>
      </template>
    </AppModal>
  </AppContent>
</template>
