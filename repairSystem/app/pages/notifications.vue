<script setup lang="ts">
import type { AppNotification } from '~/types'

const { t } = useLocale()

const { data, pending, error, refresh } = await useAsyncData('notifications', () =>
  apiFetch<AppNotification[]>('/notifications')
)

const errorMessage = computed(() => {
  const err = error.value as { data?: { message?: string } } | null
  return err?.data?.message || error.value?.message || t('retry')
})

function iconTheme(n: AppNotification) {
  return n.iconTheme ? `text-${n.iconTheme}` : ''
}
</script>

<template>
  <AppContent
    :title="t('notifications')"
    :breadcrumbs="[{ label: t('home'), to: '/' }, { label: t('notifications') }]"
  >
    <div class="row">
      <div class="col-lg-8">
        <div class="card">
          <div class="card-header">
            <h3 class="card-title">
              All Notifications
            </h3>
            <div class="card-tools">
              <span
                v-if="data?.length"
                class="badge text-bg-primary"
              >{{ data.length }} {{ t('new') }}</span>
            </div>
          </div>

          <div class="card-body table-responsive p-0">
            <div
              v-if="error"
              class="alert alert-danger m-3"
            >
              {{ errorMessage }}
              <button
                type="button"
                class="btn btn-sm btn-outline-danger ms-2"
                @click="() => refresh()"
              >
                Retry
              </button>
            </div>

            <div
              v-else-if="pending"
              class="text-center py-4"
            >
              <div class="spinner-border spinner-border-sm text-primary" />
            </div>

            <ul
              v-else-if="data?.length"
              class="list-group list-group-flush mb-0"
            >
              <li
                v-for="n in data"
                :key="n.id"
                class="list-group-item d-flex align-items-center gap-3 py-3"
              >
                <span
                  class="badge rounded-circle p-2 text-bg-light border"
                >
                  <i
                    class="bi fs-5"
                    :class="[n.icon, iconTheme(n)]"
                  />
                </span>
                <div class="flex-grow-1">
                  <NuxtLink
                    v-if="n.url"
                    :to="n.url"
                    class="text-decoration-none"
                  >{{ n.text }}</NuxtLink>
                  <template v-else>
                    {{ n.text }}
                  </template>
                  <div class="text-muted small">
                    <i class="bi bi-clock me-1" />{{ n.time }}
                  </div>
                </div>
                <NuxtLink
                  v-if="n.url"
                  :to="n.url"
                  class="btn btn-sm btn-outline-secondary"
                  title="View"
                >
                  <i class="bi bi-eye" />
                </NuxtLink>
              </li>
            </ul>

            <div
              v-else
              class="text-center text-muted py-4"
            >
              {{ t('noNotifications') }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </AppContent>
</template>
