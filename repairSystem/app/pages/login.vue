<script setup lang="ts">
definePageMeta({ layout: 'auth' })
const { t } = useLocale()

const email = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)
const { login } = useAuth()

async function onSubmit() {
  error.value = ''
  loading.value = true
  try {
    await login(email.value, password.value)
    await navigateTo('/')
  } catch (e) {
    const err = e as { data?: { message?: string }, message?: string }
    error.value = err.data?.message || err.message || 'Login failed. Please check your credentials.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="card shadow-sm login-card overflow-hidden">
    <div class="card-body p-5">
      <div class="text-center mb-4">
        <img
          src="/assets/img/AdminLTELogo.png"
          alt="Repair System"
          class="auth-logo mb-3"
        >
        <h2 class="h4 fw-bold mb-2 text-white">
          {{ t('loginTitle') }}
        </h2>
        <p class="text-muted mb-0">
          {{ t('loginSubtitle') }}
        </p>
      </div>

      <div
        v-if="error"
        class="alert alert-danger py-2"
        role="alert"
      >
        {{ error }}
      </div>

      <form
        novalidate
        @submit.prevent="onSubmit"
      >
        <div class="form-floating mb-3">
          <input
            id="login-email"
            v-model="email"
            type="email"
            class="form-control"
            :placeholder="t('email')"
            autocomplete="email"
            autofocus
            required
          >
          <label for="login-email">{{ t('email') }}</label>
        </div>
        <div class="form-floating mb-4">
          <input
            id="login-password"
            v-model="password"
            type="password"
            class="form-control"
            :placeholder="t('password')"
            autocomplete="current-password"
            required
          >
          <label for="login-password">{{ t('password') }}</label>
        </div>
        <div class="d-grid gap-2 mb-3">
          <button
            type="submit"
            class="btn btn-primary btn-lg"
            :disabled="loading"
          >
            <span
              v-if="loading"
              class="spinner-border spinner-border-sm me-2 text-white"
            />
            {{ loading ? t('signingIn') : t('signIn') }}
          </button>
        </div>
        <p class="text-center text-muted small mb-0">
          {{ t('loginHint') }}
        </p>
      </form>
    </div>
  </div>
</template>
