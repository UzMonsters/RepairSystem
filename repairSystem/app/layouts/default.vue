<script setup>
useLteBehaviors()

onMounted(() => {
  document.body.classList.add('layout-fixed', 'sidebar-expand-lg', 'fixed-header', 'fixed-sidebar', 'bg-body-tertiary')
})
</script>

<template>
  <div class="app-wrapper">
    <AppTopbar />
    <AppSidebar />
    <main class="app-main">
      <slot />
    </main>
  </div>
</template>
