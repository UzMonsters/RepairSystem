<script setup lang="ts">
import type { LanguageCode, NotificationSummary, Page, UserSettings } from '~/types'
import { getApiErrorMessage } from '~/utils/api'

const { user, logout, avatarObjectUrl } = useAuth()
const { locale, setLocale, t } = useLocale()
const { isDark, toggleTheme } = useTheme()

const displayName = computed(() => user.value?.fullName || 'Administrator')
const initials = computed(() => displayName.value.split(' ').map(p => p[0]).join('').slice(0, 2).toUpperCase())

const localeLabel = computed(() => locale.value === 'ru' ? 'RU' : locale.value === 'en' ? 'EN' : 'UZ')
const notifications = ref<NotificationSummary[]>([])
const globalSearch = ref('')
const changingLanguage = ref(false)

function toggleSidebar() {
  const mobile = window.innerWidth <= 992
  const body = document.body

  if (mobile) {
    body.classList.toggle('sidebar-open')
    body.classList.toggle('sidebar-collapse', !body.classList.contains('sidebar-open'))
    return
  }

  body.classList.toggle('sidebar-collapse')
  body.classList.remove('sidebar-open')
}

async function submitGlobalSearch() {
  const value = globalSearch.value.trim()
  if (!value) return
  await navigateTo({ path: '/admin/requests', query: { search: value } })
}

async function changeLanguage(lang: LanguageCode) {
  if (changingLanguage.value || locale.value === lang.toLowerCase()) return

  changingLanguage.value = true
  try {
    const settings = await apiFetch<UserSettings>('/settings/me')
    await apiFetch<UserSettings>('/settings/me', {
      method: 'PUT',
      body: { ...settings, language: lang }
    })
    setLocale(lang.toLowerCase())
    await refreshNuxtData()
  } catch (e) {
    console.error(getApiErrorMessage(e, 'Failed to change language.'))
  } finally {
    changingLanguage.value = false
  }
}

async function loadNotifications() {
  try {
    const page = await apiFetch<Page<NotificationSummary>>('/notifications', { query: { page: 0, size: 5 } })
    notifications.value = page.content ?? []
  } catch (e) {
    void e
  }
}

onMounted(async () => {
  await loadNotifications()
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
            @click="toggleSidebar"
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
        <li class="nav-item">
          <button
            type="button"
            class="nav-link"
            title="Toggle theme"
            aria-label="Toggle theme"
            @click="toggleTheme"
          >
            <i
              class="bi"
              :class="isDark ? 'bi-moon-stars' : 'bi-sun'"
            />
          </button>
        </li>
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
                :disabled="changingLanguage"
                @click="changeLanguage('UZ')"
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
                :disabled="changingLanguage"
                @click="changeLanguage('EN')"
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
                :disabled="changingLanguage"
                @click="changeLanguage('RU')"
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
            <img
              v-if="avatarObjectUrl"
              :src="avatarObjectUrl"
              alt=""
              class="user-image rounded-circle shadow object-fit-cover"
              style="width: 32px; height: 32px;"
            >
            <span
              v-else
              class="user-image d-inline-flex align-items-center justify-content-center rounded-circle bg-primary text-white shadow"
              style="width: 32px; height: 32px; font-size: 12px; font-weight: 600;"
            >{{ initials }}</span>
            <span class="d-none d-md-inline">{{ displayName }}</span>
          </a>
          <ul class="dropdown-menu dropdown-menu-lg dropdown-menu-end">
            <li>
              <NuxtLink
                to="/admin/profile"
                class="dropdown-item"
              >
                <i class="bi bi-person me-2" />{{ t('profile') || 'Profile' }}
              </NuxtLink>
            </li>
            <li>
              <NuxtLink
                to="/admin/settings"
                class="dropdown-item"
              >
                <i class="bi bi-gear me-2" />{{ t('settings') }}
              </NuxtLink>
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
