<script setup lang="ts">
import type { Category, Page } from '~/types'
import { getApiErrorMessage } from '~/utils/api'

const { t, locale } = useLocale()
const page = ref(1)
const size = ref(10)

const query = computed(() => ({
  page: page.value - 1,
  size: size.value
}))

const { data, pending, error, refresh } = await useAsyncData('categories-list', () =>
  apiFetch<Page<Category>>('/categories', { query: query.value })
)

const errorMessage = computed(() => {
  const err = error.value as { data?: { message?: string } } | null
  return err?.data?.message || error.value?.message || 'Failed to load categories.'
})

const rows = computed(() => data.value?.content ?? [])
const totalElements = computed(() => data.value?.totalElements ?? 0)
const totalPages = computed(() => data.value?.totalPages ?? 1)

function goToPage(target: number) {
  page.value = target
  refresh()
}

function changeSize(s: number) {
  size.value = s
  page.value = 1
  refresh()
}

function localizedName(c: Category) {
  if (locale.value === 'ru') return c.nameRu || c.nameEn
  if (locale.value === 'en') return c.nameEn || c.nameRu
  return c.nameUz || c.nameRu || c.nameEn
}

const editingId = ref<number | null>(null)
const form = ref({
  nameEn: '',
  nameRu: '',
  nameUz: '',
  descriptionEn: '',
  descriptionRu: '',
  descriptionUz: '',
  displayOrder: 0
})
const saving = ref(false)
const saveError = ref('')

function openCreate() {
  editingId.value = null
  form.value = { nameEn: '', nameRu: '', nameUz: '', descriptionEn: '', descriptionRu: '', descriptionUz: '', displayOrder: 0 }
  saveError.value = ''
  showModal('category-modal')
}

function openEdit(c: Category) {
  editingId.value = c.id
  form.value = {
    nameEn: c.nameEn ?? '',
    nameRu: c.nameRu ?? '',
    nameUz: c.nameUz ?? '',
    descriptionEn: '',
    descriptionRu: '',
    descriptionUz: '',
    displayOrder: c.displayOrder ?? 0
  }
  saveError.value = ''
  showModal('category-modal')
}

async function save() {
  saving.value = true
  saveError.value = ''
  try {
    const body = {
      nameEn: form.value.nameEn.trim(),
      nameRu: form.value.nameRu.trim(),
      nameUz: form.value.nameUz.trim(),
      descriptionEn: form.value.descriptionEn.trim() || undefined,
      descriptionRu: form.value.descriptionRu.trim() || undefined,
      descriptionUz: form.value.descriptionUz.trim() || undefined,
      displayOrder: form.value.displayOrder
    }
    if (editingId.value == null) {
      await apiFetch('/categories', { method: 'POST', body })
    } else {
      await apiFetch(`/categories/${editingId.value}`, { method: 'PUT', body })
    }
    hideModal('category-modal')
    await refresh()
  } catch (e) {
    saveError.value = getApiErrorMessage(e, 'Failed to save category.')
  } finally {
    saving.value = false
  }
}

const togglingId = ref<number | null>(null)

async function toggleActive(c: Category) {
  togglingId.value = c.id
  try {
    await apiFetch(`/categories/${c.id}/activation`, { method: 'PATCH', body: { active: !c.active } })
    await refresh()
  } catch (e) {
    saveError.value = getApiErrorMessage(e, 'Failed to change category status.')
  } finally {
    togglingId.value = null
  }
}
</script>

<template>
  <AppContent
    :title="t('categories')"
    :breadcrumbs="[{ label: t('home'), to: '/' }, { label: t('categories') }]"
  >
    <div class="card">
      <div class="card-header">
        <div class="d-flex align-items-center justify-content-between">
          <h3 class="card-title mb-0">
            {{ t('categories') }}
          </h3>
          <button
            type="button"
            class="btn btn-sm btn-primary"
            @click="openCreate"
          >
            <i class="bi bi-plus-lg me-1" />{{ t('newCategory') }}
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
              <th>{{ t('name') }}</th>
              <th>{{ t('nameEn') }}</th>
              <th>{{ t('nameRu') }}</th>
              <th>{{ t('nameUz') }}</th>
              <th>{{ t('displayOrder') }}</th>
              <th>{{ t('active') }}</th>
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
                  <i class="bi bi-tags" />
                  <p>{{ t('noCategoriesFound') }}</p>
                </div>
              </td>
            </tr>
            <tr
              v-for="c in rows"
              :key="c.id"
            >
              <td>{{ c.id }}</td>
              <td class="fw-semibold">
                {{ localizedName(c) }}
              </td>
              <td>{{ c.nameEn || '-' }}</td>
              <td>{{ c.nameRu || '-' }}</td>
              <td>{{ c.nameUz || '-' }}</td>
              <td>{{ c.displayOrder ?? '-' }}</td>
              <td>
                <span
                  class="badge"
                  :class="c.active ? 'text-bg-success' : 'text-bg-secondary'"
                >{{ t(c.active ? 'active' : 'inactive') }}</span>
              </td>
              <td class="text-end text-nowrap">
                <button
                  type="button"
                  class="btn btn-sm btn-outline-secondary"
                  :title="t('edit')"
                  @click="openEdit(c)"
                >
                  <i class="bi bi-pencil" />
                </button>
                <button
                  type="button"
                  class="btn btn-sm btn-outline-secondary ms-1"
                  :title="t('active')"
                  :disabled="togglingId === c.id"
                  @click="toggleActive(c)"
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
        :page-sizes="[10, 25, 50, 100]"
        @update:page="goToPage"
        @update:size="changeSize"
      />
    </div>

    <AppModal
      id="category-modal"
      :title="editingId == null ? t('newCategory') : t('editCategory')"
    >
      <form @submit.prevent="save">
        <div class="mb-3">
          <label
            for="category-en"
            class="form-label"
          >{{ t('nameEn') }}</label>
          <input
            id="category-en"
            v-model="form.nameEn"
            type="text"
            class="form-control"
            required
          >
        </div>
        <div class="mb-3">
          <label
            for="category-ru"
            class="form-label"
          >{{ t('nameRu') }}</label>
          <input
            id="category-ru"
            v-model="form.nameRu"
            type="text"
            class="form-control"
            required
          >
        </div>
        <div class="mb-3">
          <label
            for="category-uz"
            class="form-label"
          >{{ t('nameUz') }}</label>
          <input
            id="category-uz"
            v-model="form.nameUz"
            type="text"
            class="form-control"
            required
          >
        </div>
        <div class="mb-3">
          <label
            for="category-order"
            class="form-label"
          >{{ t('displayOrder') }}</label>
          <input
            id="category-order"
            v-model.number="form.displayOrder"
            type="number"
            min="0"
            class="form-control"
          >
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
