<script setup lang="ts">
import type { NotificationSummary, Page } from '~/types'

const { user, logout } = useAuth()
const { locale, setLocale, t } = useLocale()

const displayName = computed(() => user.value?.fullName || 'Administrator')
const role = computed(() => user.value?.role || '')
const email = computed(() => user.value?.email || '')
const initials = computed(() => displayName.value.split(' ').map(p => p[0]).join('').slice(0, 2).toUpperCase())
const localeLabel = computed(() => locale.value === 'ru' ? 'RU' : locale.value === 'en' ? 'EN' : 'UZ')
const notifications = ref<NotificationSummary[]>([])
const globalSearch = ref('')

async function submitGlobalSearch() {
  const value = globalSearch.value.trim()
  if (!value) return
  await navigateTo({ path: '/admin/requests', query: { search: value } })
}

onMounted(async () => {
  try {
    const page = await apiFetch<Page<NotificationSummary>>('/notifications', { query: { page: 0, size: 5 } })
    notifications.value = page.content ?? []
  } catch (e) {
    void e
  }
})
</script>

<template>
  <nav class="app-header navbar navbar-expand">
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
        <li class="nav-item app-header-search-item">
          <form
            class="app-header-search input-group input-group-sm"
            @submit.prevent="submitGlobalSearch"
          >
            <input
              v-model="globalSearch"
              type="search"
              class="form-control"
              :placeholder="t('search')"
            >
            <button
              type="submit"
              class="btn btn-outline-secondary"
              :aria-label="t('search')"
            >
              <i class="bi bi-search" />
            </button>
          </form>
        </li>
        <NavNotifications
          :notifications="notifications"
          see-all-url="/admin/notifications"
          :see-all-text="t('viewAllNotifications')"
        />
        <FullscreenToggle />

        <li class="nav-item dropdown">
          <button
            type="button"
            class="nav-link"
            data-bs-toggle="dropdown"
            aria-expanded="false"
          >
            <span class="badge text-bg-secondary">{{ localeLabel }}</span>
          </button>
          <ul class="dropdown-menu dropdown-menu-end">
            <li>
              <button
                type="button"
                class="dropdown-item d-flex align-items-center"
                @click="setLocale('uz')"
              >
                <span class="me-2">UZ</span>
                {{ t('uzbek') }}
                <i
                  v-if="locale === 'uz'"
                  class="bi bi-check-lg ms-auto"
                />
              </button>
            </li>
            <li>
              <button
                type="button"
                class="dropdown-item d-flex align-items-center"
                @click="setLocale('en')"
              >
                <span class="me-2">EN</span>
                English
                <i
                  v-if="locale === 'en'"
                  class="bi bi-check-lg ms-auto"
                />
              </button>
            </li>
            <li>
              <button
                type="button"
                class="dropdown-item d-flex align-items-center"
                @click="setLocale('ru')"
              >
                <span class="me-2">RU</span>
                {{ t('russian') }}
                <i
                  v-if="locale === 'ru'"
                  class="bi bi-check-lg ms-auto"
                />
              </button>
            </li>
          </ul>
        </li>

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
              >{{ t('signOut') }}</a>
            </li>
          </ul>
        </li>
      </ul>
    </div>
  </nav>
</template>
