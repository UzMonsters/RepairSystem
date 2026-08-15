<script setup lang="ts">
import type { Category, Page } from '~/types'
import { getApiErrorMessage } from '~/utils/api'

const { t, locale } = useLocale()
const activeLanguageTab = ref<'en' | 'ru' | 'uz'>('en')
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
  return getApiErrorMessage(error.value, 'Failed to load categories.')
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
  if (c.name) return c.name
  if (locale.value === 'ru') return c.nameRu || c.nameEn
  if (locale.value === 'en') return c.nameEn || c.nameRu
  return c.nameUz || c.nameRu || c.nameEn
}

function localizedDescription(c: Category) {
  if (c.description) return c.description
  if (locale.value === 'ru') return c.descriptionRu || c.descriptionEn || c.descriptionUz || '-'
  if (locale.value === 'en') return c.descriptionEn || c.descriptionRu || c.descriptionUz || '-'
  return c.descriptionUz || c.descriptionRu || c.descriptionEn || '-'
}

const editingId = ref<number | null>(null)
const form = ref({
  nameEn: '',
  nameRu: '',
  nameUz: '',
  descriptionEn: '',
  descriptionRu: '',
  descriptionUz: ''
})
const saving = ref(false)
const saveError = ref('')

function openCreate() {
  editingId.value = null
  activeLanguageTab.value = locale.value
  form.value = { nameEn: '', nameRu: '', nameUz: '', descriptionEn: '', descriptionRu: '', descriptionUz: '' }
  saveError.value = ''
  showModal('category-modal')
}

async function openEdit(c: Category) {
  editingId.value = c.id
  activeLanguageTab.value = locale.value
  form.value = {
    nameEn: c.nameEn ?? '',
    nameRu: c.nameRu ?? '',
    nameUz: c.nameUz ?? '',
    descriptionEn: c.descriptionEn ?? '',
    descriptionRu: c.descriptionRu ?? '',
    descriptionUz: c.descriptionUz ?? ''
  }
  saveError.value = ''
  try {
    const detail = await apiFetch<Category>(`/categories/${c.id}`)
    form.value.descriptionEn = detail.descriptionEn ?? ''
    form.value.descriptionRu = detail.descriptionRu ?? ''
    form.value.descriptionUz = detail.descriptionUz ?? ''
  } catch (e) {
    saveError.value = getApiErrorMessage(e, 'Failed to load category details.')
    return
  }
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
      descriptionUz: form.value.descriptionUz.trim() || undefined
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
    :breadcrumbs="[{ label: t('home'), to: '/admin' }, { label: t('categories') }]"
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
              <th>{{ t('description') }}</th>
              <th>{{ t('active') }}</th>
              <th class="text-end">
                {{ t('actions') }}
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
            <tr v-else-if="!rows.length">
              <td
                colspan="5"
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
              <td class="text-muted category-description-cell">
                {{ localizedDescription(c) }}
              </td>
              <td>
                <span class="form-check form-switch d-inline-block align-middle mb-0">
                  <input
                    class="form-check-input"
                    type="checkbox"
                    role="switch"
                    :checked="!!c.active"
                    :disabled="togglingId === c.id"
                    :aria-label="`Toggle ${localizedName(c)}`"
                    @change="toggleActive(c)"
                  >
                </span>
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
                <NuxtLink
                  :to="`/admin/categories/${c.id}`"
                  class="btn btn-sm btn-outline-secondary ms-1"
                  :title="t('view')"
                >
                  <i class="bi bi-eye" />
                </NuxtLink>
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
        <ul class="nav nav-tabs category-language-tabs mb-3">
          <li class="nav-item">
            <button
              type="button"
              class="nav-link"
              :class="{ active: activeLanguageTab === 'en' }"
              @click="activeLanguageTab = 'en'"
            >
              {{ t('english') }}
            </button>
          </li>
          <li class="nav-item">
            <button
              type="button"
              class="nav-link"
              :class="{ active: activeLanguageTab === 'ru' }"
              @click="activeLanguageTab = 'ru'"
            >
              {{ t('russian') }}
            </button>
          </li>
          <li class="nav-item">
            <button
              type="button"
              class="nav-link"
              :class="{ active: activeLanguageTab === 'uz' }"
              @click="activeLanguageTab = 'uz'"
            >
              {{ t('uzbek') }}
            </button>
          </li>
        </ul>
        <div
          v-if="activeLanguageTab === 'en'"
          class="mb-3"
        >
          <label
            for="category-en"
            class="form-label"
          >{{ t('name') }}</label>
          <input
            id="category-en"
            v-model="form.nameEn"
            type="text"
            class="form-control"
            required
          >
          <label
            for="category-description-en"
            class="form-label mt-3"
          >{{ t('description') }}</label>
          <textarea
            id="category-description-en"
            v-model="form.descriptionEn"
            class="form-control"
            rows="3"
            maxlength="500"
          />
        </div>
        <div
          v-else-if="activeLanguageTab === 'ru'"
          class="mb-3"
        >
          <label
            for="category-ru"
            class="form-label"
          >{{ t('name') }}</label>
          <input
            id="category-ru"
            v-model="form.nameRu"
            type="text"
            class="form-control"
            required
          >
          <label
            for="category-description-ru"
            class="form-label mt-3"
          >{{ t('description') }}</label>
          <textarea
            id="category-description-ru"
            v-model="form.descriptionRu"
            class="form-control"
            rows="3"
            maxlength="500"
          />
        </div>
        <div
          v-else
          class="mb-3"
        >
          <label
            for="category-uz"
            class="form-label"
          >{{ t('name') }}</label>
          <input
            id="category-uz"
            v-model="form.nameUz"
            type="text"
            class="form-control"
            required
          >
          <label
            for="category-description-uz"
            class="form-label mt-3"
          >{{ t('description') }}</label>
          <textarea
            id="category-description-uz"
            v-model="form.descriptionUz"
            class="form-control"
            rows="3"
            maxlength="500"
          />
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
