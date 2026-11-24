// 日時はUTCのISO文字列で受け取り、指定タイムゾーンで表示する。
export const browserZone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Tokyo'

export function formatInZone(iso, tz, opts = {}) {
  return new Intl.DateTimeFormat('ja-JP', {
    timeZone: tz, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', ...opts,
  }).format(new Date(iso))
}

/** 指定タイムゾーンでの yyyy-mm-dd */
export function dateKey(iso, tz) {
  const p = new Intl.DateTimeFormat('en-CA', { timeZone: tz, year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(iso))
  return p
}

function offsetMinutes(tz, date) {
  const parts = Object.fromEntries(
    new Intl.DateTimeFormat('en-US', {
      timeZone: tz, hourCycle: 'h23', year: 'numeric', month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit', second: '2-digit',
    }).formatToParts(date).map((x) => [x.type, x.value]),
  )
  const asUtc = Date.UTC(+parts.year, +parts.month - 1, +parts.day, +parts.hour, +parts.minute, +parts.second)
  return Math.round((asUtc - Math.floor(date.getTime() / 1000) * 1000) / 60000)
}

/** "2026-12-01T19:00"(tzの壁時計) → "2026-12-01T19:00:00+09:00" */
export function localToOffsetISO(local, tz) {
  const [d, t] = local.split('T')
  const [y, m, day] = d.split('-').map(Number)
  const [h, mi] = t.split(':').map(Number)
  const guess = Date.UTC(y, m - 1, day, h, mi)
  let off = offsetMinutes(tz, new Date(guess))
  off = offsetMinutes(tz, new Date(guess - off * 60000)) // DST境界の補正
  const sign = off >= 0 ? '+' : '-'
  const a = Math.abs(off)
  const hh = String(Math.floor(a / 60)).padStart(2, '0')
  const mm = String(a % 60).padStart(2, '0')
  return `${local}:00${sign}${hh}:${mm}`
}
