<script setup>
useLteBehaviors()

onMounted(() => {
  useAuth().init()
  document.body.classList.add('layout-fixed', 'sidebar-expand-lg', 'sidebar-mini', 'fixed-sidebar', 'bg-body-tertiary')
})
</script>

<template>
  <div class="app-wrapper">
    <AppTopbar />
    <AppSidebar />
    <AppToastContainer />
    <main class="app-main">
      <slot />
    </main>
  </div>
</template>
