const fs = require('fs')

// --- Update index.vue ---
let idxCode = fs.readFileSync('app/pages/admin/technicians/index.vue', 'utf8')
idxCode = idxCode.replace(
  'function openMessage(_tech: Technician) {\n  useToast().addToast(\'Функция отправки сообщений (Message) будет доступна в следующем обновлении.\', \'info\')\n}',
  'const router = useRouter()\nfunction openMessage(tech: Technician) {\n  router.push(`/admin/technicians/${tech.id}`)\n}'
)
// Fix duplicate router if exists
if (idxCode.match(/const router = useRouter\(\)/g)?.length > 1) {
  idxCode = idxCode.replace('const router = useRouter()\nconst router = useRouter()', 'const router = useRouter()')
}
fs.writeFileSync('app/pages/admin/technicians/index.vue', idxCode)
