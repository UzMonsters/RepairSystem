<script setup lang="ts">
import type { ChatMessage, ConversationSummary, RealtimeEvent } from '~/types/realtime'
import { getApiErrorCode, getApiErrorMessage } from '~/utils/api'

const props = defineProps<{
  requestId: number
  requestStatus?: string
}>()

const { t } = useLocale()
const realtime = useRealtime()
const conversation = ref<ConversationSummary | null>(null)
const messages = ref<ChatMessage[]>([])
const text = ref('')
const loading = ref(true)
const sending = ref(false)
const error = ref('')
const typing = ref(false)
let stopRealtime: (() => boolean) | undefined

const readOnly = computed(() => props.requestStatus === 'COMPLETED'
  || props.requestStatus === 'CANCELLED'
  || conversation.value?.status === 'CLOSED')

function messageTime(value?: string) {
  if (!value) return ''
  return new Intl.DateTimeFormat(undefined, { hour: '2-digit', minute: '2-digit' }).format(new Date(value))
}

function messageId() {
  return globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

async function markRead() {
  const latest = messages.value[messages.value.length - 1]
  if (!conversation.value || !latest) return
  await apiFetch(`/conversations/${conversation.value.id}/read`, {
    method: 'POST',
    body: { messageId: latest.id }
  })
}

async function loadChat() {
  loading.value = true
  error.value = ''
  try {
    conversation.value = await apiFetch<ConversationSummary>(
      `/conversations/requests/${props.requestId}/technician-manager`,
      { method: 'POST' }
    )
    const page = await apiFetch<{ content: ChatMessage[] }>(
      `/conversations/${conversation.value.id}/messages`,
      { query: { page: 0, size: 100 } }
    )
    messages.value = [...(page.content ?? [])].reverse()
    await markRead()
  } catch (e) {
    error.value = getApiErrorMessage(e, t('chatUnavailable'))
  } finally {
    loading.value = false
  }
}

async function sendMessage() {
  const value = text.value.trim()
  if (!value || !conversation.value || readOnly.value || sending.value) return
  sending.value = true
  error.value = ''
  try {
    const sent = await apiFetch<ChatMessage>(`/conversations/${conversation.value.id}/messages`, {
      method: 'POST',
      body: {
        conversationId: conversation.value.id,
        clientMessageId: messageId(),
        type: 'TEXT',
        text: value,
        attachmentId: null,
        replyToMessageId: null
      }
    })
    if (!messages.value.some(message => message.id === sent.id)) messages.value.push(sent)
    text.value = ''
  } catch (e) {
    if (getApiErrorCode(e) === 'CONVERSATION_READ_ONLY') {
      error.value = t('chatReadOnly')
    } else {
      error.value = getApiErrorMessage(e, t('chatSendFailed'))
    }
  } finally {
    sending.value = false
  }
}

function handleRealtime(event: RealtimeEvent) {
  const payload = event.payload as Partial<ChatMessage> & { conversationId?: number, typing?: boolean }
  if (payload.conversationId !== conversation.value?.id) return
  if (event.type === 'CHAT_MESSAGE_CREATED' && payload.id && !messages.value.some(message => message.id === payload.id)) {
    messages.value.push(payload as ChatMessage)
    void markRead()
  }
  if (event.type === 'CHAT_TYPING_STARTED') typing.value = true
  if (event.type === 'CHAT_TYPING_STOPPED' || event.type === 'CHAT_MESSAGE_CREATED') typing.value = false
}

onMounted(async () => {
  await loadChat()
  stopRealtime = realtime.subscribe(handleRealtime)
})

onBeforeUnmount(() => {
  stopRealtime?.()
})
</script>

<template>
  <div class="card manager-chat-card">
    <div class="card-header d-flex align-items-center justify-content-between">
      <h3 class="card-title mb-0">
        <i class="bi bi-chat-dots me-2" />{{ t('technicianChat') }}
      </h3>
      <span
        v-if="conversation?.status === 'CLOSED'"
        class="badge text-bg-secondary"
      >{{ t('closed') }}</span>
    </div>
    <div class="card-body">
      <div
        v-if="loading"
        class="text-center py-4"
      >
        <div class="spinner-border spinner-border-sm text-primary" />
      </div>
      <div
        v-else-if="error"
        class="alert alert-warning mb-0"
      >
        {{ error }}
      </div>
      <template v-else>
        <div class="manager-chat-messages">
          <div
            v-if="!messages.length"
            class="text-muted text-center py-3"
          >
            {{ t('noChatMessages') }}
          </div>
          <div
            v-for="message in messages"
            :key="message.id"
            class="manager-chat-message"
            :class="{ 'is-own': message.senderType === 'STAFF' }"
          >
            <div>{{ message.text || `[${message.messageType}]` }}</div>
            <small>{{ messageTime(message.createdAt) }}</small>
          </div>
        </div>
        <div
          v-if="typing"
          class="small text-muted mt-2"
        >
          {{ t('technicianTyping') }}
        </div>
        <form
          class="input-group mt-3"
          @submit.prevent="sendMessage"
        >
          <input
            v-model="text"
            class="form-control"
            :disabled="readOnly || sending"
            :placeholder="readOnly ? t('chatReadOnly') : t('writeMessage')"
            maxlength="4000"
            @input="typing = false"
          >
          <button
            type="submit"
            class="btn btn-primary"
            :disabled="readOnly || sending || !text.trim()"
          >
            <i class="bi bi-send" />
          </button>
        </form>
      </template>
    </div>
  </div>
</template>

<style scoped>
.manager-chat-messages {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-height: 20rem;
  overflow-y: auto;
}

.manager-chat-message {
  align-self: flex-start;
  max-width: 85%;
  padding: 0.55rem 0.75rem;
  border-radius: 0.75rem;
  background: var(--rs-panel-2, #f1f5f9);
  color: var(--rs-text, #1e293b);
  border: 1px solid var(--rs-border, #e2e8f0);
}

.manager-chat-message.is-own {
  align-self: flex-end;
  background: var(--rs-primary-soft, #e0e7ff);
  border-color: transparent;
}

.manager-chat-message small {
  display: block;
  margin-top: 0.2rem;
  color: var(--rs-muted, #64748b);
  font-size: 0.75rem;
}
</style>
