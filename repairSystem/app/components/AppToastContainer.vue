<script setup lang="ts">
import { useToast } from '~/composables/useToast'

const { toasts, removeToast } = useToast()
</script>

<template>
  <div
    class="toast-container position-fixed top-0 end-0 p-3"
    style="z-index: 9999;"
  >
    <TransitionGroup name="toast">
      <div
        v-for="t in toasts"
        :key="t.id"
        class="toast show align-items-center text-bg-light border-0 mb-2 shadow"
        :class="t.type === 'success' ? 'text-bg-success text-white' : t.type === 'error' ? 'text-bg-danger text-white' : ''"
        role="alert"
        aria-live="assertive"
        aria-atomic="true"
      >
        <div class="d-flex">
          <div class="toast-body">
            {{ t.message }}
          </div>
          <button
            type="button"
            class="btn-close me-2 m-auto"
            :class="(t.type === 'success' || t.type === 'error') ? 'btn-close-white' : ''"
            aria-label="Close"
            @click="removeToast(t.id)"
          />
        </div>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}
.toast-enter-from {
  opacity: 0;
  transform: translateX(50px);
}
.toast-leave-to {
  opacity: 0;
  transform: translateX(50px);
}
</style>
