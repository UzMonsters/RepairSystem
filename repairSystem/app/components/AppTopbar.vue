<script setup lang="ts">
import type { AppNotification } from '~/types'

const { user, logout } = useAuth()

const displayName = computed(() => user.value?.fullName || 'Administrator')
const role = computed(() => user.value?.role || '')
const email = computed(() => user.value?.email || '')
const initials = computed(() => displayName.value.split(' ').map(p => p[0]).join('').slice(0, 2).toUpperCase())

const notifications = ref<AppNotification[]>([])

onMounted(async () => {
  try {
    notifications.value = await apiFetch<AppNotification[]>('/notifications')
  } catch (e) {
    void e
  }
})
</script>

<template>
  <nav class="app-header navbar navbar-expand bg-body">
    <div class="container-fluid">
      <ul class="navbar-nav">
        <li class="nav-item">
          <button
            type="button"
            class="nav-link"
            title="Toggle sidebar"
            aria-label="Toggle sidebar"
            data-lte-toggle="sidebar"
          >
            <i class="bi bi-list" />
          </button>
        </li>
      </ul>

      <ul class="navbar-nav ms-auto">
        <NavNotifications
          :notifications="notifications"
          see-all-url="/notifications"
          see-all-text="View All Notifications"
        />
        <FullscreenToggle />
        <ColorModeToggle />

        <li class="nav-item dropdown user-menu">
          <a
            href="#"
            class="nav-link dropdown-toggle"
            data-bs-toggle="dropdown"
            @click.prevent
          >
            <span
              class="user-image d-inline-flex align-items-center justify-content-center rounded-circle bg-primary text-white shadow"
              style="width: 32px; height: 32px; font-size: 12px; font-weight: 600;"
            >{{ initials }}</span>
            <span class="d-none d-md-inline">{{ displayName }}</span>
          </a>
          <ul class="dropdown-menu dropdown-menu-lg dropdown-menu-end">
            <li class="user-header text-bg-primary">
              <span
                class="d-inline-flex align-items-center justify-content-center rounded-circle shadow"
                style="width: 48px; height: 48px; font-size: 18px; font-weight: 600; background: rgba(255, 255, 255, 0.25);"
              >{{ initials }}</span>
              <p>
                {{ displayName }}
                <small>{{ role }}</small>
                <small class="d-block">{{ email }}</small>
              </p>
            </li>
            <li class="user-footer">
              <a
                href="#"
                class="btn btn-outline-danger w-100"
                @click.prevent="logout"
              >Sign out</a>
            </li>
          </ul>
        </li>
      </ul>
    </div>
  </nav>
</template>
