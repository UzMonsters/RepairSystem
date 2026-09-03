const fs = require('fs')

let code = fs.readFileSync('app/pages/admin/technicians/[id].vue', 'utf8')

// fix imports
code = code.replace(
  'import type { Technician } from \'~/types\'',
  'import type { Technician } from \'~/types\'\nimport type { ConversationSummary, ParticipantSummary } from \'~/types/realtime\''
)

// fix any
code = code.replace(
  'apiFetch<any>(\'/conversations\', { query: { size: 100 } })',
  'apiFetch<{ content: ConversationSummary[] }>(\'/conversations\', { query: { size: 100 } })'
)

code = code.replace(
  '(conversationsData.value?.content || []).filter((c: any) => ',
  '(conversationsData.value?.content || []).filter((c: ConversationSummary) => '
)

code = code.replace(
  'c.participants.some((p: any) => p.actorType === \'TECHNICIAN\' && p.actorId === id)',
  'c.participants?.some((p: ParticipantSummary) => p.actorType === \'TECHNICIAN\' && p.actorId === id)'
)

// fix duplicated end tags
// It currently has:
//     </div>
//     </div>
//   </AppContent>
// </template>

code = code.replace(/<\/div>\s*<\/div>\s*<\/AppContent>/, '  </div>\n  </AppContent>')

fs.writeFileSync('app/pages/admin/technicians/[id].vue', code)
