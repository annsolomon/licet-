import React from 'react';

/** Line chart of percentages (0-100). points = [{ label, value }] */
export function LineChart({ points, height = 170, min = 50 }) {
  const w = 640;
  const pad = { l: 34, r: 12, t: 12, b: 26 };
  const usable = points.filter((p) => p.value !== null && p.value !== undefined);
  if (usable.length < 2) return <p className="muted">Not enough days yet.</p>;
  const x = (i) => pad.l + (i * (w - pad.l - pad.r)) / (points.length - 1);
  const y = (v) => pad.t + ((100 - Math.max(min, v)) * (height - pad.t - pad.b)) / (100 - min);
  const d = points.map((p, i) => `${i ? 'L' : 'M'}${x(i).toFixed(1)},${y(p.value).toFixed(1)}`).join(' ');
  const ticks = [min, (min + 100) / 2, 100];
  return (
    <svg viewBox={`0 0 ${w} ${height}`} className="chart" role="img" aria-label="Attendance trend">
      {ticks.map((t) => (
        <g key={t}>
          <line x1={pad.l} x2={w - pad.r} y1={y(t)} y2={y(t)} className="grid" />
          <text x={pad.l - 6} y={y(t) + 4} textAnchor="end" className="axis">{Math.round(t)}</text>
        </g>
      ))}
      <path d={d} className="line" />
      {points.map((p, i) => (
        <g key={p.label}>
          <circle cx={x(i)} cy={y(p.value)} r="3.5" className="dot"><title>{`${p.label}: ${p.value}%`}</title></circle>
          {(i % 2 === 0 || points.length < 8) && <text x={x(i)} y={height - 8} textAnchor="middle" className="axis">{p.label}</text>}
        </g>
      ))}
    </svg>
  );
}

/** Vertical bars of percentages. items = [{ label, value }] */
export function Bars({ items, height = 150, min = 50 }) {
  const w = 640;
  const pad = { l: 30, r: 8, t: 16, b: 24 };
  const bw = (w - pad.l - pad.r) / items.length;
  const h = (v) => ((Math.max(min, v ?? min) - min) * (height - pad.t - pad.b)) / (100 - min);
  return (
    <svg viewBox={`0 0 ${w} ${height}`} className="chart" role="img" aria-label="Attendance by period">
      {items.map((it, i) => {
        const bh = it.value == null ? 0 : h(it.value);
        const cls = it.value == null ? 'bar-none' : it.value >= 85 ? 'bar-good' : it.value >= 75 ? 'bar-ok' : 'bar-bad';
        return (
          <g key={it.label}>
            <rect x={pad.l + i * bw + 6} y={height - pad.b - bh} width={bw - 12} height={bh} rx="5" className={cls}>
              <title>{`${it.label}: ${it.value ?? 'no data'}%`}</title>
            </rect>
            <text x={pad.l + i * bw + bw / 2} y={height - pad.b - bh - 4} textAnchor="middle" className="axis">{it.value == null ? '' : Math.round(it.value)}</text>
            <text x={pad.l + i * bw + bw / 2} y={height - 8} textAnchor="middle" className="axis">{it.label}</text>
          </g>
        );
      })}
    </svg>
  );
}

/** Ring showing a percentage. */
export function Ring({ value, size = 132, label = 'attendance' }) {
  const r = 52;
  const c = 2 * Math.PI * r;
  const v = value == null ? 0 : Math.min(100, Math.max(0, value));
  const cls = value == null ? 'ring-none' : value >= 85 ? 'ring-good' : value >= 75 ? 'ring-ok' : 'ring-bad';
  return (
    <svg width={size} height={size} viewBox="0 0 132 132" role="img" aria-label={`${label} ${value ?? 'n/a'} percent`}>
      <circle cx="66" cy="66" r={r} className="ring-track" />
      <circle cx="66" cy="66" r={r} className={`ring-fill ${cls}`} strokeDasharray={`${(v / 100) * c} ${c}`} transform="rotate(-90 66 66)" />
      <text x="66" y="68" textAnchor="middle" className="ring-value">{value == null ? '–' : `${Number(value).toFixed(1).replace(/\.0$/, '')}%`}</text>
      <text x="66" y="86" textAnchor="middle" className="ring-label">{label}</text>
    </svg>
  );
}
