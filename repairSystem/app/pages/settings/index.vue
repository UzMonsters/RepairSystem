<script setup lang="ts">
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
    title="Settings"
    :breadcrumbs="[{ label: 'Home', to: '/' }, { label: 'Settings' }]"
  >
    <div class="row">
      <div class="col-lg-6">
        <div class="card">
          <div class="card-header">
            <h3 class="card-title">
              Telegram Integration
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
              v-if="saved"
              class="alert alert-success py-2"
            >
              Settings saved successfully.
            </div>
            <form @submit.prevent="saveSettings">
              <div class="mb-3">
                <label
                  for="telegram-username"
                  class="form-label"
                >Telegram Bot Username</label>
                <div class="input-group">
                  <span class="input-group-text">@</span>
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
                  The username customers use to find the bot in Telegram.
                </div>
              </div>
              <button
                type="submit"
                class="btn btn-primary"
                :disabled="saving || loading"
              >
                {{ saving ? 'Saving...' : 'Save Settings' }}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  </AppContent>
</template>
