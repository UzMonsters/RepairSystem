<script setup lang="ts">
import type { Page } from '~/types'
import type { ConversationSummary } from '~/types/realtime'
import { formatDate } from '~/utils/date'

const { t } = useLocale()
const { data, pending, error } = useAsyncData('conversations', () =>
  apiFetch<Page<ConversationSummary>>('/conversations', { query: { page: 0, size: 50, sort: 'updatedAt,desc' } })
)

const activeChatId = ref<number | undefined>(undefined)

const conversations = computed(() => data.value?.content?.filter(c => c.conversationType === 'TECHNICIAN_MANAGER') || [])

function selectChat(id: number) {
  activeChatId.value = id
}

function chatTitle(c: ConversationSummary) {
  const other = c.participants?.find(p => p.actorType !== 'STAFF')
  const name = other?.displayName || 'Неизвестно'
  const req = c.requestNumber || `Заявка #${c.repairRequestId}`
  return `${name} (${req})`
}`
}
</script>

<template>
  <AppContent
    :title="t('chats') || 'Сообщения'"
    :breadcrumbs="[{ label: t('home'), to: '/admin' }, { label: t('chats') || 'Сообщения' }]"
  >
    <div class="row g-4 h-100">
      <!-- Left sidebar: chat list -->
      <div class="col-lg-4">
        <div
          class="card dash-card h-100"
          style="min-height: 500px;"
        >
          <div class="card-header border-bottom">
            <h3 class="card-title mb-0">
              Чат
            </h3>
          </div>
          <div
            class="card-body p-0"
            style="overflow-y: auto; max-height: 600px;"
          >
            <div
              v-if="pending"
              class="text-center p-4"
            >
              <span class="spinner-border text-primary" />
            </div>
            <div
              v-else-if="error"
              class="alert alert-danger m-3"
            >
              Ошибка загрузки чатов.
            </div>
            <div
              v-else-if="!conversations.length"
              class="text-center p-4 text-muted"
            >
              Нет активных чатов.
            </div>
            <div
              v-else
              class="list-group list-group-flush"
            >
              <button
                v-for="c in conversations"
                :key="c.id"
                class="list-group-item list-group-item-action border-bottom p-3 d-flex flex-column gap-1 chat-list-item"
                :class="{ active: activeChatId === c.id }"
                @click="selectChat(c.id)"
              >
                <div class="d-flex justify-content-between align-items-center w-100">
                  <strong class="text-truncate">{{ chatTitle(c) }}</strong>
                  <small
                    v-if="c.updatedAt"
                    class="text-muted text-nowrap ms-2"
                  >{{ formatDate(c.updatedAt, true) }}</small>
                </div>
                <div class="d-flex justify-content-between align-items-center w-100">
                  <span class="text-truncate text-muted small">
                    <span
                      v-if="c.conversationType === 'CUSTOMER_TECHNICIAN'"
                      class="badge bg-info me-1"
                    >Клиент</span>
                    <span
                      v-if="c.conversationType === 'TECHNICIAN_MANAGER'"
                      class="badge bg-secondary me-1"
                    >Мастер</span>
                    {{ c.lastMessage?.text || 'Нет сообщений' }}
                  </span>
                  <span
                    v-if="c.unreadCount > 0"
                    class="badge bg-danger rounded-pill"
                  >{{ c.unreadCount }}</span>
                </div>
              </button>
            </div>
          </div>
        </div>
      </div>
      <!-- Right area: active chat -->
      <div class="col-lg-8">
        <div
          v-if="activeChatId"
          class="h-100"
          style="min-height: 500px;"
        >
          <ManagerChatBox :conversation-id="activeChatId" />
        </div>
        <div
          v-else
          class="card dash-card h-100 d-flex align-items-center justify-content-center"
          style="min-height: 500px;"
        >
          <div class="text-muted text-center">
            <i class="bi bi-chat-dots fs-1 mb-2" />
            <p>Выберите чат для начала переписки</p>
          </div>
        </div>
      </div>
    </div>
  </AppContent>
</template>

<style scoped>
.chat-list-item {
  transition: background-color 0.2s, color 0.2s;
}
.chat-list-item:hover:not(.active) {
  background-color: #6f42c1 !important;
  color: #fff !important;
}
.chat-list-item:hover:not(.active) .text-muted {
  color: #e9ecef !important;
}
</style>
