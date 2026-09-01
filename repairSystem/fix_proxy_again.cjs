const fs = require('fs');
let code = fs.readFileSync('server/routes/api/[...path].ts', 'utf8');

const target = "let body: BodyInit | Record<string, unknown> | undefined = ['GET', 'HEAD'].includes(method)\n    ? undefined\n    : await readRawBody(event, false) as unknown as BodyInit";

const replacement = `  if (isBinaryDownload) {
    return proxyRequest(event, \`\${config.backendUrl}/api/v1/\${path}\`, {
      headers: forwardHeaders
    })
  }

  let body: BodyInit | Record<string, unknown> | undefined = ['GET', 'HEAD'].includes(method)
    ? undefined
    : await readRawBody(event, false) as unknown as BodyInit`;

code = code.replace(target, replacement);

fs.writeFileSync('server/routes/api/[...path].ts', code);
