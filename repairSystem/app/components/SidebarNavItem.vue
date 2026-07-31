<script setup>
const props = defineProps({
  item: { type: Object, required: true },
  currentPath: { type: String, default: '/' }
})

function isActiveHref(href) {
  if (href === '/') return props.currentPath === '/'
  return props.currentPath === href || props.currentPath.startsWith(href + '/')
}

function hasActiveDescendant(node) {
  if (node.type === 'item') return isActiveHref(node.href)
  if (node.type === 'group') return node.children.some(hasActiveDescendant)
  return false
}

const isItemActive = computed(() => props.item.type === 'item' && isActiveHref(props.item.href))

const groupActive = computed(() => props.item.type === 'group' && props.item.children.some(hasActiveDescendant))

const isOpen = ref(groupActive.value)

watch(groupActive, (active) => {
  if (active) isOpen.value = true
})

const { logout } = useAuth()

async function signOut() {
  await logout()
}
</script>

<template>
  <li
    v-if="item.type === 'header'"
    class="nav-header"
  >
    {{ item.text }}
  </li>

  <li
    v-else-if="item.type === 'item'"
    :class="['nav-item', isItemActive && 'active']"
  >
    <a
      v-if="item.action === 'signout'"
      href="#"
      class="nav-link"
      @click.prevent="signOut"
    >
      <i
        v-if="item.icon"
        :class="['nav-icon', item.icon, item.iconColor && `text-${item.iconColor}`]"
      />
      <p>{{ item.text }}</p>
    </a>
    <NuxtLink
      v-else
      :to="item.href"
      :target="item.target"
      :class="['nav-link', isItemActive && 'active']"
    >
      <i
        v-if="item.icon"
        :class="['nav-icon', item.icon, item.iconColor && `text-${item.iconColor}`]"
      />
      <p>
        {{ item.text }}
        <span
          v-if="item.badge != null"
          :class="`nav-badge badge text-bg-${item.badgeColor || 'secondary'} ms-auto`"
        >{{ item.badge }}</span>
      </p>
    </NuxtLink>
  </li>

  <li
    v-else
    :class="['nav-item', isOpen && 'menu-open']"
  >
    <button
      type="button"
      class="nav-link"
      :aria-expanded="isOpen"
      @click="isOpen = !isOpen"
    >
      <i
        v-if="item.icon"
        :class="['nav-icon', item.icon]"
      />
      <p>
        {{ item.text }}
        <i class="nav-arrow bi bi-chevron-right" />
        <span
          v-if="item.badge != null"
          :class="`nav-badge badge text-bg-${item.badgeColor || 'secondary'} ms-auto me-3`"
        >{{ item.badge }}</span>
      </p>
    </button>
    <ul class="nav nav-treeview">
      <SidebarNavItem
        v-for="child in item.children"
        :key="child.type === 'item' ? child.href : `${child.type}:${child.text}`"
        :item="child"
        :current-path="currentPath"
      />
    </ul>
  </li>
</template>
