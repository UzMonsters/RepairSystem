const fs = require('fs')

let code = fs.readFileSync('server/routes/api/[...path].ts', 'utf8')

const targetRegex = /let body: BodyInit([^]+?)await readRawBody\(event, false\) as unknown as BodyInit/

const replacement = `  if (isBinaryDownload) {
    return proxyRequest(event, \`\${config.backendUrl}/api/v1/\${path}\`, {
      headers: forwardHeaders
    })
  }

  let body: BodyInit$1await readRawBody(event, false) as unknown as BodyInit`

code = code.replace(targetRegex, replacement)

fs.writeFileSync('server/routes/api/[...path].ts', code)
