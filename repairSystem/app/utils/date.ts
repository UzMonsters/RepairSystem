export function formatDate(value?: string | Date | null, withTime = false) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'

  const dateFormat = import.meta.client ? localStorage.getItem('repair_date_format') : null
  const timeFormat = import.meta.client ? localStorage.getItem('repair_time_format') : null
  const dateOptions = dateFormat === 'YYYY_MM_DD'
    ? { year: 'numeric' as const, month: '2-digit' as const, day: '2-digit' as const }
    : { day: '2-digit' as const, month: '2-digit' as const, year: 'numeric' as const }

  return new Intl.DateTimeFormat(dateFormat === 'YYYY_MM_DD' ? 'sv-SE' : 'en-GB', {
    ...dateOptions,
    ...(withTime ? { hour: '2-digit', minute: '2-digit', hour12: timeFormat === 'HOUR_12' } : {})
  }).format(date)
}
