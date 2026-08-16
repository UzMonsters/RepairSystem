<script setup lang="ts">
import { getApiErrorMessage } from '~/utils/api'

definePageMeta({ layout: 'auth' })
const { t } = useLocale()

const email = ref('')
const password = ref('')
const showPassword = ref(false)
const rememberMe = ref(false)
const error = ref('')
const loading = ref(false)
const { login } = useAuth()

async function onSubmit() {
  error.value = ''
  const missingEmail = !email.value.trim()
  const missingPassword = !password.value.trim()
  if (missingEmail || missingPassword) {
    error.value = t('loginFieldsRequired')
    return
  }
  loading.value = true
  try {
    await login(email.value, password.value, rememberMe.value)
    await navigateTo('/admin')
  } catch (e) {
    error.value = getApiErrorMessage(e, 'Login failed. Please check your credentials.')
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
        <h2 class="h4 fw-bold mb-2 auth-title">
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
        <div class="login-field mb-3">
          <label for="login-email">{{ t('email') }}</label>
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
        </div>
        <div class="login-field login-password-field mb-4">
          <label for="login-password">{{ t('password') }}</label>
          <input
            id="login-password"
            v-model="password"
            :type="showPassword ? 'text' : 'password'"
            class="form-control pe-5"
            :placeholder="t('password')"
            autocomplete="current-password"
            required
          >
          <button
            type="button"
            class="password-toggle"
            :aria-label="showPassword ? t('hidePassword') : t('showPassword')"
            :title="showPassword ? t('hidePassword') : t('showPassword')"
            @click="showPassword = !showPassword"
          >
            <i :class="showPassword ? 'bi bi-eye-slash' : 'bi bi-eye'" />
          </button>
        </div>
        <div class="form-check mb-4">
          <input
            id="login-remember-me"
            v-model="rememberMe"
            type="checkbox"
            class="form-check-input"
          >
          <label for="login-remember-me" class="form-check-label text-muted">
            {{ t('rememberMe') }}
          </label>
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
      </form>
    </div>
  </div>
</template>

<style scoped>
.login-password-field {
  position: relative;
}

.login-field > label {
  display: block;
  margin-bottom: .45rem;
  color: var(--rs-text-2);
  font-size: .9rem;
  font-weight: 600;
}

.login-field > .form-control {
  min-height: 48px;
  padding: .75rem 1rem;
  border-radius: 10px;
}

.login-field > .form-control::placeholder {
  color: var(--rs-muted);
  opacity: .85;
}

.password-toggle {
  position: absolute;
  /* Align with the input itself, below the field label. */
  top: 53px;
  right: 10px;
  z-index: 3;
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--rs-muted);
  transform: translateY(-50%);
  cursor: pointer;
}

.password-toggle:hover,
.password-toggle:focus-visible {
  color: var(--rs-primary);
  background: var(--rs-primary-soft);
}
</style>
