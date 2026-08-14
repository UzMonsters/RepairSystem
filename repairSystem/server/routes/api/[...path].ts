export default defineEventHandler(async (event): Promise<unknown> => {
  const config = useRuntimeConfig(event)
  const path = event.context.params?.path
  if (!path) throw createError({ statusCode: 404, statusMessage: 'Not Found' })

  const method = event.method
  const query = getQuery(event)
  const incoming = getRequestHeaders(event)

  const forwardHeaders: Record<string, string> = { accept: incoming.accept || 'application/json' }
  if (incoming['accept-language']) forwardHeaders['accept-language'] = incoming['accept-language']
  if (incoming.authorization) forwardHeaders.authorization = incoming.authorization
  if (incoming['content-type']) forwardHeaders['content-type'] = incoming['content-type']
  if (incoming.cookie) forwardHeaders.cookie = incoming.cookie

  const body = ['GET', 'HEAD'].includes(method) ? undefined : await readRawBody(event, false)

  try {
    const res = await $fetch.raw<unknown>(`${config.backendUrl}/api/v1/${path}`, {
      method,
      query,
      body,
      headers: forwardHeaders,
      retry: 0
    })

    setResponseStatus(event, res.status)
    const contentType = res.headers.get('content-type')
    if (contentType) setResponseHeader(event, 'content-type', contentType)
    const setCookies = typeof res.headers.getSetCookie === 'function'
      ? res.headers.getSetCookie()
      : []
    if (setCookies.length) setResponseHeaders(event, { 'set-cookie': setCookies })
    return res._data
  } catch (error) {
    const err = error as {
      statusCode?: number
      status?: number
      statusMessage?: string
      data?: unknown
      message?: string
    }
    throw createError({
      statusCode: err.statusCode ?? err.status ?? 502,
      statusMessage: err.statusMessage,
      data: err.data ?? { message: err.message ?? 'Backend unavailable' }
    })
  }
})
