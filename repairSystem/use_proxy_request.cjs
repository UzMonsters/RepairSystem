const fs = require('fs')

let code = fs.readFileSync('server/routes/api/[...path].ts', 'utf8')

// We will inject a proxyRequest path for binary downloads
const proxyCode = `
  if (isBinaryDownload) {
    return proxyRequest(event, \`\${config.backendUrl}/api/v1/\${path}\`, {
      headers: forwardHeaders
    })
  }

  let body = ['GET', 'HEAD'].includes(method)
`

code = code.replace('let body: BodyInit | Record<string, unknown> | undefined = [\'GET\', \'HEAD\'].includes(method)\n    ? undefined\n    : await readRawBody(event, false) as unknown as BodyInit', proxyCode.trim() + '\n    ? undefined\n    : await readRawBody(event, false) as unknown as BodyInit')

code = code.replace(
  'responseType: isBinaryDownload ? \'arrayBuffer\' : \'json\'',
  'responseType: \'json\''
)

code = code.replace(
  'if (isBinaryDownload) {\n      return res._data\n    }',
  ''
)

fs.writeFileSync('server/routes/api/[...path].ts', code)
