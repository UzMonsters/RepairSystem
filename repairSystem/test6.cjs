const fs = require('fs')

let code = fs.readFileSync('../backend/src/main/java/com/example/darks/repair_auto/chat/application/ChatService.java', 'utf8')
const lines = code.split('\n')
lines.forEach((l, i) => {
  if (l.includes('remove') || l.includes('leave') || l.includes('inactive')) {
    console.log(`Line ${i}: ${l}`)
  }
})
