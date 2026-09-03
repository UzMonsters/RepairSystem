<script setup lang="ts">
import type { ChatMessage, ChatMessagePayload, ChatReadPayload, ChatTypingPayload, ConversationSummary, RealtimeEvent } from '~/types/realtime'
import { getApiErrorCode, getApiErrorMessage } from '~/utils/api'

const props = defineProps<{
  requestId?: number
  conversationId?: number
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
let typingTimeout: ReturnType<typeof setTimeout> | undefined

const readOnly = computed(() => props.requestStatus === 'COMPLETED'
  || props.requestStatus === 'CANCELLED'
  || conversation.value?.status === 'CLOSED')

function sendTyping(typingState: boolean) {
  if (!conversation.value) return
  realtime.publish('/app/chat.typing', {
    conversationId: conversation.value.id,
    typing: typingState
  })
}

function handleTyping() {
  sendTyping(true)
  if (typingTimeout) clearTimeout(typingTimeout)
  typingTimeout = setTimeout(() => sendTyping(false), 3000)
}

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
          if (props.conversationId) {
        conversation.value = await apiFetch<ConversationSummary>(`/conversations/${props.conversationId}`)
      } else if (props.requestId) {
        conversation.value = await apiFetch<ConversationSummary>(
          `/conversations/requests/${props.requestId}/technician-manager`,
          { method: 'POST' }
        )
      } else {
        throw new Error('Either conversationId or requestId is required')
      }
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
  if (event.type === 'CHAT_MESSAGE_CREATED') {
    const payload = event.payload as ChatMessagePayload
    if (payload.conversationId !== conversation.value?.id) return

    const normalizedMessage: ChatMessage = {
      id: payload.messageId,
      conversationId: payload.conversationId,
      senderType: payload.senderType,
      senderId: payload.senderId,
      clientMessageId: payload.clientMessageId || '',
      messageType: payload.messageType,
      text: payload.text || '',
      attachmentId: payload.attachmentId,
      replyToMessageId: payload.replyToMessageId,
      createdAt: payload.createdAt || new Date().toISOString()
    }

    if (!messages.value.some(m => m.id === normalizedMessage.id || (m.clientMessageId && m.clientMessageId === normalizedMessage.clientMessageId))) {
      messages.value.push(normalizedMessage)
      void markRead()
    }
    typing.value = false
  }

  if (event.type === 'CHAT_MESSAGE_READ') {
    const payload = event.payload as ChatReadPayload
    if (payload.conversationId === conversation.value?.id) {
      void payload
    }
  }

  if (event.type === 'CHAT_TYPING_STARTED') {
    const payload = event.payload as ChatTypingPayload
    if (payload.conversationId === conversation.value?.id) typing.value = true
  }

  if (event.type === 'CHAT_TYPING_STOPPED') {
    const payload = event.payload as ChatTypingPayload
    if (payload.conversationId === conversation.value?.id) typing.value = false
  }
}

onMounted(async () => {
  await loadChat()
  stopRealtime = realtime.subscribe(handleRealtime)
})

onBeforeUnmount(() => {
  stopRealtime?.()
  if (typingTimeout) clearTimeout(typingTimeout)
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
            @input="handleTyping"
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
