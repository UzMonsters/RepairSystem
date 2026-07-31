<script setup>
defineProps({
  notifications: { type: Array, default: () => [] },
  seeAllUrl: { type: String, default: '#' },
  seeAllText: { type: String, default: 'See All Notifications' }
})
</script>

<template>
  <li class="nav-item dropdown">
    <a
      class="nav-link"
      data-bs-toggle="dropdown"
      href="#"
      @click.prevent
    >
      <i class="bi bi-bell-fill" />
      <span
        v-if="notifications.length"
        class="navbar-badge badge text-bg-warning"
      >{{ notifications.length }}</span>
    </a>
    <div class="dropdown-menu dropdown-menu-lg dropdown-menu-end">
      <span class="dropdown-item dropdown-header">{{ notifications.length }} Notifications</span>
      <template
        v-for="(n, idx) in notifications"
        :key="idx"
      >
        <div class="dropdown-divider" />
        <a
          :href="n.url || '#'"
          class="dropdown-item"
        >
          <i :class="[n.icon || 'bi-info-circle', n.iconTheme && `text-${n.iconTheme}`, 'me-2']" />
          {{ n.text }}
          <span
            v-if="n.time"
            class="float-end text-secondary fs-7"
          >{{ n.time }}</span>
        </a>
      </template>
      <div class="dropdown-divider" />
      <a
        :href="seeAllUrl"
        class="dropdown-item dropdown-footer"
      >{{ seeAllText }}</a>
    </div>
  </li>
</template>
