const fs = require('fs')

let code = fs.readFileSync('app/pages/admin/technicians/[id].vue', 'utf8')

// We need to fetch conversations and filter for the technician.
// We'll add this to the <script setup>
const scriptAddition = `
const { data: conversationsData } = await useAsyncData(\`tech-convs-\${id}\`, () =>
  apiFetch<any>('/conversations', { query: { size: 100 } })
)
const techConversations = computed(() => {
  return (conversationsData.value?.content || []).filter((c: any) => 
    c.conversationType === 'TECHNICIAN_MANAGER' &&
    c.participants.some((p: any) => p.actorType === 'TECHNICIAN' && p.actorId === id)
  )
})
const selectedChatId = ref<number | null>(null)
watchEffect(() => {
  if (techConversations.value.length > 0 && !selectedChatId.value) {
    selectedChatId.value = techConversations.value[0].id
  }
})
`

code = code.replace('function formatDate(value?: string) {\n  return formatApiDate(value)\n}', 'function formatDate(value?: string) {\n  return formatApiDate(value)\n}\n' + scriptAddition)

// Update template
const newChatHTML = `
      <!-- Chat / Messages -->
      <div class="col-lg-7">
        <div class="card h-100 d-flex flex-column" style="min-height: 600px;">
          <div class="card-header d-flex justify-content-between align-items-center">
            <h3 class="card-title mb-0">{{ t('chat') }}</h3>
            <select v-if="techConversations.length > 0" class="form-select form-select-sm w-auto" v-model="selectedChatId">
              <option v-for="c in techConversations" :key="c.id" :value="c.id">
                {{ c.requestNumber }} ({{ formatDate(c.updatedAt) }})
              </option>
            </select>
          </div>
          <div class="card-body p-0 flex-grow-1 position-relative">
            <ManagerChatBox v-if="selectedChatId" :key="selectedChatId" :conversation-id="selectedChatId" />
            <div v-else class="text-muted text-center p-4 d-flex flex-column justify-content-center h-100">
              <i class="bi bi-chat-dots fs-1 mb-2 d-block" />
              <p>{{ t('noChatMessages') }}</p>
            </div>
          </div>
        </div>
      </div>
`

code = code.replace(/<!-- Chat \/ Messages Placeholder -->[\s\S]*?<\/div>\s*<\/div>/, newChatHTML)

fs.writeFileSync('app/pages/admin/technicians/[id].vue', code)
