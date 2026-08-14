export function formatDate(value?: string | Date | null, withTime = false) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'

  const dateFormat = import.meta.client ? localStorage.getItem('repair_date_format') : null
  const timeFormat = import.meta.client ? localStorage.getItem('repair_time_format') : null
  const day = String(date.getDate()).padStart(2, '0')
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const year = String(date.getFullYear())

  const datePart = dateFormat === 'YYYY_MM_DD'
    ? `${year}-${month}-${day}`
    : dateFormat === 'DD_MM_YYYY'
      ? `${day}.${month}.${year}`
      : `${day}/${month}/${year}`

  if (!withTime) return datePart

  const time = new Intl.DateTimeFormat('en-GB', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: timeFormat === 'HOUR_12'
  }).format(date)

  return `${datePart}, ${time}`
}
