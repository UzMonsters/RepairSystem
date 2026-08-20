<script setup lang="ts">
defineProps<{
  id: string
  title: string
  size?: '' | 'sm' | 'lg' | 'xl'
}>()
</script>

<template>
  <div
    :id="id"
    class="modal fade"
    tabindex="-1"
    aria-hidden="true"
  >
    <div
      class="modal-dialog modal-dialog-centered"
      :class="size && `modal-${size}`"
    >
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title">
            {{ title }}
          </h5>
          <button
            type="button"
            class="btn-close"
            data-bs-dismiss="modal"
            aria-label="Close"
          />
        </div>
        <div class="modal-body">
          <slot />
        </div>
        <div
          v-if="$slots.footer"
          class="modal-footer"
        >
          <slot name="footer" />
        </div>
      </div>
    </div>
  </div>
</template>
