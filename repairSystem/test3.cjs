const fs = require('fs')

let code = fs.readFileSync('../backend/src/main/java/com/example/darks/repair_auto/chat/application/ChatService.java', 'utf8')
const lines = code.split('\n')
console.log(lines.slice(265, 285).join('\n'))
