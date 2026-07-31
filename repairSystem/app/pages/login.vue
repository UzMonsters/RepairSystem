<script setup lang="ts">
definePageMeta({ layout: 'auth' })

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
  <div class="card card-outline card-primary">
    <div class="card-header text-center">
      <h1 class="h3 mb-0 fw-bold">
        Repair Service CRM
      </h1>
    </div>
    <div class="card-body login-card-body">
      <p class="text-center text-muted mb-4">
        Sign in to start your session
      </p>

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
        <div class="input-group mb-3">
          <span class="input-group-text"><i class="bi bi-envelope" /></span>
          <input
            v-model="email"
            type="email"
            class="form-control"
            placeholder="Email"
            autocomplete="email"
            autofocus
            required
          >
        </div>
        <div class="input-group mb-3">
          <span class="input-group-text"><i class="bi bi-lock-fill" /></span>
          <input
            v-model="password"
            type="password"
            class="form-control"
            placeholder="Password"
            autocomplete="current-password"
            required
          >
        </div>
        <div class="d-grid gap-2">
          <button
            type="submit"
            class="btn btn-primary"
            :disabled="loading"
          >
            <span
              v-if="loading"
              class="spinner-border spinner-border-sm me-1"
            />
            {{ loading ? 'Signing in...' : 'Sign In' }}
          </button>
        </div>
        <p class="text-center text-muted small mt-3 mb-0">
          Demo mode — any email and password work.
        </p>
      </form>
    </div>
  </div>
</template>
