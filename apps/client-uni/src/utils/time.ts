/** API 的无偏移时间均为北京时间；显式偏移时间也统一展示北京时间。 */
export function formatBeijingTime(value?: string) {
  if (!value) return '—'
  const normalized = value.replace(' ', 'T')
  const date = new Date(/[zZ]|[+-]\d{2}:?\d{2}$/.test(normalized) ? normalized : `${normalized}+08:00`)
  if (Number.isNaN(date.getTime())) return '—'
  const bj = new Date(date.getTime() + 8 * 3600000)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${bj.getUTCFullYear()}-${pad(bj.getUTCMonth() + 1)}-${pad(bj.getUTCDate())} ${pad(bj.getUTCHours())}:${pad(bj.getUTCMinutes())}`
}
