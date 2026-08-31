const fs = require('fs')

let code = fs.readFileSync('../backend/src/main/java/com/example/darks/repair_auto/chat/application/ChatService.java', 'utf8')
const lines = code.split('\n')
const start = lines.findIndex(l => l.includes('public PageResponse<ChatMessageResponse> getMessageHistory'))
console.log(lines.slice(start, start + 30).join('\n'))
