<script setup>
onMounted(() => {
  document.body.classList.remove('layout-fixed', 'sidebar-expand-lg', 'fixed-header', 'fixed-sidebar', 'bg-body-tertiary')
})
</script>

<template>
  <div class="login-page">
    <div class="login-box">
      <slot />
    </div>
  </div>
</template>
