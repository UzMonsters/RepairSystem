<script setup>
import { menu } from '~/lib/menu'

const { user } = useAuth()
const { t } = useLocale()
const route = useRoute()
const sidebarWrapper = ref(null)
const search = ref('')
let osInstance = null

const baseMenu = computed(() => {
  return menu.filter((item) => {
    if (item.type !== 'item') return true
    if (!item.roles?.length) return true
    return item.roles.includes(user.value?.role || 'MANAGER')
  })
})

const visibleMenu = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return baseMenu.value
  return baseMenu.value.map((item) => {
    if (item.type === 'header') return item
    if (item.type === 'item') {
      const label = (t(item.text) || item.text).toLowerCase()
      if (label.includes(q)) return item
      return null
    }
    const children = (item.children ?? []).filter((c) => {
      if (c.type === 'item') return (t(c.text) || c.text).toLowerCase().includes(q)
      return true
    })
    if (!children.length) return null
    return { ...item, children }
  }).filter(Boolean)
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
        to="/admin"
        class="brand-link"
      >
        <img
          src="/assets/img/AdminLTELogo.png"
          alt="Logo"
          class="brand-image"
        >
        <span class="brand-text">Repair<b>System</b></span>
      </NuxtLink>
    </div>

    <div
      ref="sidebarWrapper"
      class="sidebar-wrapper"
    >
      <div class="sidebar-search">
        <div class="input-group">
          <span class="input-group-text">
            <i class="bi bi-search" />
          </span>
          <input
            v-model="search"
            type="search"
            class="form-control"
            placeholder="Search for..."
            aria-label="Search menu"
          >
        </div>
      </div>

      <nav
        class="mt-1"
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
