<script setup>
import { menu } from '~/lib/menu'

const { user } = useAuth()
const route = useRoute()
const sidebarWrapper = ref(null)
let osInstance = null

const visibleMenu = computed(() => {
  return menu.filter(item => {
    if (item.type !== 'item') return true
    if (!item.roles?.length) return true
    return item.roles.includes(user.value?.role || 'MANAGER')
  })
})

onMounted(async () => {
  if (window.innerWidth <= 992 || !sidebarWrapper.value) return
  try {
    const { OverlayScrollbars } = await import('overlayscrollbars')
    osInstance = OverlayScrollbars(sidebarWrapper.value, {
      scrollbars: { theme: 'os-theme-light', autoHide: 'leave', clickScroll: true }
    })
  } catch (e) {
    void e
  }
})

onBeforeUnmount(() => {
  osInstance?.destroy?.()
})

function closeSidebar() {
  document.body.classList.remove('sidebar-open')
  document.body.classList.add('sidebar-collapse')
}
</script>

<template>
  <aside
    class="app-sidebar shadow"
    data-bs-theme="dark"
  >
    <div class="sidebar-brand">
      <NuxtLink
        to="/"
        class="brand-link"
      >
        <img
          src="/assets/img/AdminLTELogo.png"
          alt="Logo"
          class="brand-image opacity-75 shadow"
        >
        <span class="brand-text fw-light">Repair<b>System</b></span>
      </NuxtLink>
    </div>

    <div
      ref="sidebarWrapper"
      class="sidebar-wrapper"
    >
      <nav
        class="mt-2"
        aria-label="Main navigation"
      >
        <ul
          id="navigation"
          class="nav sidebar-menu flex-column"
          data-lte-toggle="treeview"
        >
          <SidebarNavItem
            v-for="item in visibleMenu"
            :key="item.type === 'item' ? item.href : `${item.type}:${item.text}`"
            :item="item"
            :current-path="route.path"
          />
        </ul>
      </nav>
    </div>
  </aside>

  <div
    class="sidebar-overlay"
    role="presentation"
    @click="closeSidebar"
    @touchend="closeSidebar"
  />
</template>
