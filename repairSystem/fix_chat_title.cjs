const fs = require('fs')

let code = fs.readFileSync('app/pages/admin/chats/index.vue', 'utf8')

const oldTitleFunc = `function chatTitle(c: ConversationSummary) {
  const other = c.participants?.find(p => p.actorType !== 'STAFF')
  const name = other?.displayName || t('unknown', 'Неизвестно')
  const req = c.requestNumber || \`\${t('request', 'Заявка')} #\${c.repairRequestId}\`
  return \`\${name} (\${req})\`
}`

const newTitleFunc = `function chatTitle(c: ConversationSummary) {
  const other = c.participants?.find(p => p.actorType !== 'STAFF')
  return other?.displayName || t('unknown', 'Неизвестно')
}`

code = code.replace(oldTitleFunc, newTitleFunc)
fs.writeFileSync('app/pages/admin/chats/index.vue', code)
