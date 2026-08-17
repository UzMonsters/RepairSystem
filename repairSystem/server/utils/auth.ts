const ACCESS_COOKIE = 'access_token'
const REFRESH_COOKIE = 'refresh_token'

type TokenPayload = {
  accessToken?: string
  refreshToken?: string
  accessTokenExpiresIn?: number
  refreshTokenExpiresIn?: number
  rememberMe?: boolean
}

function cookieOptions(maxAge?: number) {
  return {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax' as const,
    path: '/',
    ...(maxAge === undefined ? {} : { maxAge })
  }
}

export function getAccessToken(event: H3Event) {
  return getCookie(event, ACCESS_COOKIE)
}

export function getRefreshToken(event: H3Event) {
  return getCookie(event, REFRESH_COOKIE)
}

export function storeAuthCookies(event: H3Event, payload: TokenPayload) {
  if (payload.accessToken) {
    const maxAge = payload.rememberMe ? payload.accessTokenExpiresIn : undefined
    setCookie(event, ACCESS_COOKIE, payload.accessToken, cookieOptions(maxAge))
  }
  if (payload.refreshToken) {
    const maxAge = payload.rememberMe ? payload.refreshTokenExpiresIn : undefined
    setCookie(event, REFRESH_COOKIE, payload.refreshToken, cookieOptions(maxAge))
  }
}

export function clearAuthCookies(event: H3Event) {
  deleteCookie(event, ACCESS_COOKIE, { path: '/' })
  deleteCookie(event, REFRESH_COOKIE, { path: '/' })
}

export function withoutTokens<T extends TokenPayload>(payload: T) {
  const { accessToken: _accessToken, refreshToken: _refreshToken, ...safePayload } = payload
  return safePayload
}
import type { H3Event } from 'h3'
