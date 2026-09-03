const fs = require('fs')

let code = fs.readFileSync('app/pages/admin/chats/index.vue', 'utf8')

const oldFunc = `function chatTitle(c: ConversationSummary) {
  const other = c.participants?.find(p => p.actorType !== 'STAFF')
  return other?.displayName || t('unknown', 'Неизвестно')
}`

const newFunc = `function chatTitle(c: ConversationSummary) {
  const other = c.participants?.find(p => p.actorType !== 'STAFF')
  if (other?.displayName) return other.displayName
  if (c.participants && c.participants.length > 0) {
    return c.participants.map(p => p.displayName).filter(Boolean).join(', ')
  }
  return t('unknown', 'Неизвестно')
}`

code = code.replace(oldFunc, newFunc)
fs.writeFileSync('app/pages/admin/chats/index.vue', code)
