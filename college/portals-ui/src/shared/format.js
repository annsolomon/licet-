export const pct = (v) => (v === null || v === undefined ? '–' : `${Number(v).toFixed(1).replace(/\.0$/, '')}%`);
export const level = (v, threshold = 75) => (v === null || v === undefined ? 'none' : Number(v) >= 85 ? 'good' : Number(v) >= threshold ? 'ok' : 'bad');

export function longDate(iso) {
  if (!iso) return '';
  const d = new Date(`${iso}T00:00:00`);
  return d.toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' });
}
export function shortDate(iso) {
  const d = new Date(`${iso}T00:00:00`);
  return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
}
export const STATUS_LABEL = { PRESENT: 'Present', ABSENT: 'Absent', OD: 'On duty', MEDICAL: 'Medical', OTHER: 'Other' };
export const STATUS_SHORT = { PRESENT: 'P', ABSENT: 'A', OD: 'OD', MEDICAL: 'M', OTHER: 'O' };
