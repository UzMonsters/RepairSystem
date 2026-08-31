const fs = require('fs')

let code = fs.readFileSync('app/pages/admin/requests/[id].vue', 'utf8')
const lines = code.split('\n')
console.log(lines.slice(410, 430).join('\n'))
