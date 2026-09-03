const fs = require('fs')

let code = fs.readFileSync('app/pages/admin/chats/index.vue', 'utf8')

code = code.replace(
  '<ManagerChatBox :conversation-id="activeChatId" />',
  '<ManagerChatBox :key="activeChatId" :conversation-id="activeChatId" />'
)

fs.writeFileSync('app/pages/admin/chats/index.vue', code)
