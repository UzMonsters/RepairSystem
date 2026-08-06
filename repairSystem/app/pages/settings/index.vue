<script setup lang="ts">
const { user } = useAuth()
const { t } = useLocale()
const isAdmin = computed(() => user.value?.role === 'ADMIN')
const botUsername = ref('')
const loading = ref(false)
const saving = ref(false)
const saved = ref(false)
const error = ref('')

onMounted(async () => {
  loading.value = true
  try {
    const data = await apiFetch<{ telegramBotUsername?: string }>('/settings')
    botUsername.value = data.telegramBotUsername ?? ''
  } catch (e) {
    void e
  } finally {
    loading.value = false
  }
})

async function saveSettings() {
  error.value = ''
  saved.value = false
  saving.value = true
  try {
    await apiFetch('/settings', { method: 'PUT', body: { telegramBotUsername: botUsername.value.trim() } })
    saved.value = true
  } catch (e) {
    const err = e as { data?: { message?: string }, message?: string }
    error.value = err.data?.message || err.message || 'Failed to save settings.'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <AppContent
    :title="t('settings')"
    :breadcrumbs="[{ label: t('home'), to: '/' }, { label: t('settings') }]"
  >
    <div
      v-if="!isAdmin"
      class="alert alert-warning"
    >
      {{ t('notAuthorized') }}
    </div>
    <div
      v-else
      class="row"
    >
      <div class="col-lg-6">
        <div class="card">
          <div class="card-header">
            <h3 class="card-title">
              {{ t('telegramIntegration') }}
            </h3>
          </div>
          <div class="card-body">
            <div
              v-if="error"
              class="alert alert-danger py-2"
            >
              {{ error }}
            </div>
            <div
              v-else-if="saved"
              class="alert alert-success py-2"
            >
              {{ t('savedSuccessfully') }}
            </div>
            <form @submit.prevent="saveSettings">
              <div class="mb-3">
                <label
                  for="telegram-username"
                  class="form-label"
                >{{ t('telegramBotUsername') }}</label>
                <div class="input-group">
                  <span 
                    style="background:rgba(255, 255, 255, 0.06)"  class="input-group-text">@</span>
                  <input
                    id="telegram-username"
                    v-model="botUsername"
                    type="text"
                    class="form-control"
                    placeholder="RepairServiceBot"
                    :disabled="loading"
                  >
                </div>
                <div class="form-text">
                  {{ t('telegramBotUsername') }}.
                </div>
              </div>
              <button
                type="submit"
                class="btn btn-primary"
                :disabled="saving || loading"
              >
                {{ saving ? t('saving') : t('save') }}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  </AppContent>
</template>
