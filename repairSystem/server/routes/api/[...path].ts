import {
  clearAuthCookies,
  getAccessToken,
  getRefreshToken,
  storeAuthCookies,
  withoutTokens
} from '../../utils/auth'

export default defineEventHandler(async (event): Promise<unknown> => {
  const config = useRuntimeConfig(event)
  const path = event.context.params?.path
  if (!path) throw createError({ statusCode: 404, statusMessage: 'Not Found' })

  const method = event.method
  const query = getQuery(event)
  const incoming = getRequestHeaders(event)
  const isBinaryDownload = method === 'GET'
    && (
      path === 'me/avatar'
      || path === 'mobile/me/avatar'
      || /^(?:users|customers|technicians)\/[^/]+\/avatar$/.test(path)
      || /^attachments\/[^/]+\/download$/.test(path)
      || /^mobile\/me\/attachments\/[^/]+\/download$/.test(path)
    )

  const isLogin = path === 'auth/login' && method === 'POST'
  const isRefresh = path === 'auth/refresh' && method === 'POST'
  const isLogout = path === 'auth/logout' && method === 'POST'
  const forwardHeaders: Record<string, string> = { accept: incoming.accept || 'application/json' }
  if (incoming['accept-language']) forwardHeaders['accept-language'] = incoming['accept-language']
  if (incoming.authorization) forwardHeaders.authorization = incoming.authorization
  else if (!isLogin && !isRefresh && !isLogout) {
    const accessToken = getAccessToken(event)
    if (accessToken) forwardHeaders.authorization = `Bearer ${accessToken}`
  }
  if (incoming['content-type']) forwardHeaders['content-type'] = incoming['content-type']
  if (incoming.cookie) forwardHeaders.cookie = incoming.cookie

  if (isBinaryDownload) {
    return proxyRequest(event, `${config.backendUrl}/api/v1/${path}`, {
      // Do not rewrite streaming headers: PNG, WebP and JPEG must retain the
      // content type supplied by backend so the browser can decode the avatar.
      headers: forwardHeaders
    })
  }

  let body: BodyInit | Record<string, unknown> | undefined = ['GET', 'HEAD'].includes(method)
    ? undefined
    : await readRawBody(event, false) as unknown as BodyInit
  if (isRefresh || isLogout) {
    body = { refreshToken: getRefreshToken(event) || '' }
  }

  try {
    const res = await $fetch.raw<unknown>(`${config.backendUrl}/api/v1/${path}`, {
      method,
      query,
      body,
      headers: forwardHeaders,
      retry: 0,
      timeout: 20000,
      // Image/file downloads are raw bytes, not JSON. Keep them binary while
      // all other API calls retain normal JSON parsing.
      responseType: 'json'
    })

    setResponseStatus(event, res.status)
    const contentType = res.headers.get('content-type')
    if (contentType) setResponseHeader(event, 'content-type', contentType)
    // Do not forward content-length: Nitro serializes the parsed backend
    // payload again, so the upstream length can truncate the browser response.
    for (const header of ['content-disposition', 'cache-control']) {
      const value = res.headers.get(header)
      if (value) setResponseHeader(event, header, value)
    }
    const setCookies = typeof res.headers.getSetCookie === 'function'
      ? res.headers.getSetCookie()
      : []
    if (setCookies.length) setResponseHeaders(event, { 'set-cookie': setCookies })
    if (isLogin || isRefresh) {
      const tokenPayload = res._data as Parameters<typeof storeAuthCookies>[1] & Record<string, unknown>
      storeAuthCookies(event, tokenPayload)
      return withoutTokens(tokenPayload)
    }
    if (isLogout) {
      clearAuthCookies(event)
      return res._data
    }
    if (isBinaryDownload) {
      return res._data
    }

    return res._data
  } catch (error) {
    if (isRefresh || isLogout) clearAuthCookies(event)
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
