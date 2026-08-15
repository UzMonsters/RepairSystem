<script setup lang="ts">
import { getApiErrorMessage } from '~/utils/api'

definePageMeta({ layout: 'auth' })
const { t } = useLocale()

const email = ref('')
const password = ref('')
const showPassword = ref(false)
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
    await login(email.value, password.value)
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
        <div class="form-floating login-password-field mb-4">
          <input
            id="login-password"
            v-model="password"
            :type="showPassword ? 'text' : 'password'"
            class="form-control pe-5"
            :placeholder="t('password')"
            autocomplete="current-password"
            required
          >
          <label for="login-password">{{ t('password') }}</label>
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

.password-toggle {
  position: absolute;
  top: 50%;
  right: 10px;
  z-index: 3;
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #8fa4cc;
  transform: translateY(-50%);
  cursor: pointer;
}

.password-toggle:hover,
.password-toggle:focus-visible {
  color: #fff;
  background: rgba(255, 255, 255, .1);
}
</style>
