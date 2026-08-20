<script setup>
onMounted(() => {
  document.body.classList.remove('layout-fixed', 'sidebar-expand-lg', 'fixed-header', 'fixed-sidebar', 'bg-body-tertiary')
})
</script>

<template>
  <div class="auth-wrapper min-vh-100 d-flex align-items-center justify-content-center">
    <div class="auth-box w-100 px-3 px-sm-0">
      <div class="auth-brand text-center mb-4" />
      <slot />
    </div>
  </div>
</template>
