<script setup lang="ts">
import type { LanguageCode, SystemSettings, UserSettings } from '~/types'
import { getApiErrorMessage } from '~/utils/api'

const { user } = useAuth()
const { setLocale, t } = useLocale()
const isAdmin = computed(() => user.value?.role === 'ADMIN')
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const personal = ref<UserSettings>({ language: 'UZ', dateFormat: 'DD_SLASH_MM_SLASH_YYYY', timeFormat: 'HOUR_24', theme: 'SYSTEM' })
const system = ref<SystemSettings>({ timezone: 'Asia/Tashkent', defaultLanguage: 'UZ' })
const toast = useToast()

const languages: LanguageCode[] = ['UZ', 'RU', 'EN']

onMounted(loadSettings)

async function loadSettings() {
  loading.value = true
  error.value = ''
  try {
    personal.value = await apiFetch<UserSettings>('/settings/me')
    system.value = await apiFetch<SystemSettings>('/settings/system')
  } catch (e) {
    error.value = getApiErrorMessage(e, 'Failed to load settings.')
  } finally {
    loading.value = false
  }
}

async function saveSettings() {
  saving.value = true
  error.value = ''
  try {
    const updated = await apiFetch<UserSettings>('/settings/me', { method: 'PUT', body: personal.value as unknown as Record<string, unknown> })
    personal.value = updated
    
    // Sync with global user profile
    if (user.value) {
      user.value.language = updated.language
      user.value.dateFormat = updated.dateFormat
      user.value.timeFormat = updated.timeFormat
      user.value.theme = updated.theme
    }

    setLocale(updated.language.toLowerCase())
    if (import.meta.client) {
      localStorage.setItem('repair_date_format', updated.dateFormat)
      localStorage.setItem('repair_time_format', updated.timeFormat)
    }
    if (isAdmin.value) {
      system.value = await apiFetch<SystemSettings>('/settings/system', { method: 'PUT', body: system.value as unknown as Record<string, unknown> })
    }
    await refreshNuxtData()
    toast.showSuccess(t('savedSuccessfully'))
  } catch (e) {
    toast.showError(getApiErrorMessage(e, 'Failed to save settings.'))
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <AppContent
    :title="t('settings')"
    :breadcrumbs="[{ label: t('home'), to: '/admin' }, { label: t('settings') }]"
  >
    <div
      v-if="error"
      class="alert alert-danger"
    >
      {{ error }}
    </div>
    <div
      v-if="loading"
      class="settings-loading text-center py-5"
    >
      <div
        class="spinner-border text-primary"
        role="status"
      />
    </div>
    <div
      v-else
      class="row g-4 settings-page"
    >
      <div class="col-lg-6">
        <div class="card h-100">
          <div class="card-header">
            <h3 class="card-title">
              {{ t('personalSettings') }}
            </h3>
          </div>
          <div class="card-body">
            <form @submit.prevent="saveSettings">
              <div class="mb-3">
                <label class="form-label">{{ t('language') }}</label>
                <select
                  v-model="personal.language"
                  class="form-select"
                  :disabled="loading"
                >
                  <option
                    v-for="lang in languages"
                    :key="lang"
                    :value="lang"
                  >
                    {{ t(`language.${lang}`) }}
                  </option>
                </select>
              </div>
              <div class="mb-3">
                <label class="form-label">{{ t('dateFormat') }}</label>
                <select
                  v-model="personal.dateFormat"
                  class="form-select"
                  :disabled="loading"
                >
                  <option value="DD_SLASH_MM_SLASH_YYYY">
                    dd/mm/yyyy (14/08/2026)
                  </option>
                  <option value="DD_MM_YYYY">
                    dd_mm_yyyy (14.08.2026)
                  </option>
                  <option value="YYYY_MM_DD">
                    yyyy_mm_dd (2026-08-14)
                  </option>
                </select>
              </div>
              <div class="mb-3">
                <label class="form-label">{{ t('timeFormat') }}</label>
                <select
                  v-model="personal.timeFormat"
                  class="form-select"
                  :disabled="loading"
                >
                  <option value="HOUR_24">
                    24:00
                  </option>
                  <option value="HOUR_12">
                    12:00 AM/PM
                  </option>
                </select>
              </div>
              <div class="mb-3">
                <label class="form-label">{{ t('theme') }}</label>
                <select
                  v-model="personal.theme"
                  class="form-select"
                  :disabled="loading"
                >
                  <option value="SYSTEM">
                    {{ t('themeSystem') }}
                  </option>
                  <option value="DARK">
                    {{ t('themeDark') }}
                  </option>
                  <option value="LIGHT">
                    {{ t('themeLight') }}
                  </option>
                </select>
              </div>
              <button
                class="btn btn-primary"
                type="submit"
                :disabled="saving || loading"
              >
                <span
                  v-if="saving"
                  class="spinner-border spinner-border-sm me-2"
                  role="status"
                />
                {{ saving ? t('saving') : t('save') }}
              </button>
            </form>
          </div>
        </div>
      </div>
      <div
        v-if="isAdmin"
        class="col-lg-6"
      >
        <div class="card h-100">
          <div class="card-header">
            <h3 class="card-title">
              {{ t('systemSettings') }}
            </h3>
          </div>
          <div class="card-body">
            <div class="mb-3">
              <label class="form-label">{{ t('timezone') }}</label>
              <input
                v-model="system.timezone"
                class="form-control"
                disabled
              >
            </div>
            <div>
              <label class="form-label">{{ t('defaultLanguage') }}</label>
              <select
                v-model="system.defaultLanguage"
                class="form-select"
                :disabled="!isAdmin || loading || saving"
              >
                <option
                  v-for="lang in languages"
                  :key="lang"
                  :value="lang"
                >
                  {{ t(`language.${lang}`) }}
                </option>
              </select>
            </div>
          </div>
        </div>
      </div>
    </div>
  </AppContent>
</template>
