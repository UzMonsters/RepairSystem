const fs = require('fs')

let code = fs.readFileSync('app/pages/admin/requests/[id].vue', 'utf8')
let idx = code.indexOf('<div class="col-lg-4">')
console.log(code.substring(idx - 100, idx + 50))
