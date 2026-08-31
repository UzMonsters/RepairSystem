import { defineEventHandler, createError } from 'h3'
import { getAccessToken } from '../../utils/auth'

export default defineEventHandler((event) => {
  const token = getAccessToken(event)
  if (!token) throw createError({ statusCode: 401, message: 'Unauthorized' })
  return { token }
})
