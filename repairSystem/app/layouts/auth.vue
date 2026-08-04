<script setup>
onMounted(() => {
  document.body.classList.remove('layout-fixed', 'sidebar-expand-lg', 'fixed-header', 'fixed-sidebar', 'bg-body-tertiary')
})
</script>

<template>
  <div class="auth-wrapper min-vh-100 d-flex align-items-center justify-content-center">
    <div class="auth-box w-100 px-3 px-sm-0">
      <div class="auth-brand text-center mb-4">
        <img
          src="/assets/img/AdminLTELogo.png"
          alt="Repair System"
          class="auth-logo mb-3"
        >
        <h1 class="h3 mb-1 fw-bold text-white">
          Repair System
        </h1>
        <p class="text-muted mb-0">
          Staff dashboard for repair request and technician management.
        </p>
      </div>
      <slot />
    </div>
  </div>
</template>
