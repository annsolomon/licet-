import React from 'react';
import { useApi } from '@shared/hooks.js';
import { Ring } from '@shared/charts.jsx';
import { Card, ErrorNote, Spinner } from '@shared/ui.jsx';
import { STATUS_LABEL, STATUS_SHORT, longDate } from '@shared/format.js';

export function PeriodStrip({ periods }) {
  return (
    <div className="strip">
      {periods.map((p) => (
        <div key={p.period} className={`chip st-${p.status || 'NONE'}`} title={`${p.subject || 'No class'} · ${p.status ? STATUS_LABEL[p.status] : 'not marked'}`}>
          {p.status ? STATUS_SHORT[p.status] : '·'}<small>P{p.period}</small>
        </div>
      ))}
    </div>
  );
}

export function PeriodList({ periods }) {
  return (
    <div className="plist">
      {periods.map((p) => (
        <div key={p.period} className="prow">
          <span className="pn">P{p.period}</span>
          <span><strong>{p.subject || 'No class'}</strong><br /><span className="muted small">{p.startTime}–{p.endTime}{p.faculty ? ` · ${p.faculty}` : ''}</span></span>
          <span className={`badge st-${p.status || 'NONE'}`}>{p.status ? STATUS_LABEL[p.status] : '–'}</span>
        </div>
      ))}
    </div>
  );
}

export default function Home({ api, go }) {
  const ward = useApi(() => api.get('/api/ward'), []);
  const day = useApi(() => api.get('/api/day'), []);
  if (ward.error) return <ErrorNote error={ward.error} onRetry={ward.reload} />;
  if (!ward.data || !day.data) return <Spinner />;
  const w = ward.data;
  const absent = day.data.periods.filter((p) => p.status === 'ABSENT');
  return (
    <>
      <section className="hero">
        <Ring value={w.attendance.percentage == null ? null : Number(w.attendance.percentage)} />
        <div className="hero-text">
          <h2>{w.name}</h2>
          <span className="muted">{w.deptName} · {w.className}</span>
          <span className="small">{w.attendance.present} of {w.attendance.total} periods attended</span>
          {Number(w.attendance.percentage) < w.threshold && <span className="badge lvl-bad" style={{ justifySelf: 'start' }}>Below {w.threshold} %</span>}
        </div>
      </section>
      {absent.length > 0 && (
        <div className="note note-bad" role="alert">
          <span><strong>{w.name.split(' ')[0]} was absent in {absent.length} period{absent.length > 1 ? 's' : ''}</strong> on {longDate(day.data.date)}: {absent.map((p) => `P${p.period}`).join(', ')}</span>
          <button className="btn btn-quiet" onClick={() => go('alerts')}>Alerts</button>
        </div>
      )}
      <Card title={longDate(day.data.date)} subtitle="All 8 periods of the latest day">
        <PeriodStrip periods={day.data.periods} />
        <div style={{ height: 14 }} />
        <PeriodList periods={day.data.periods} />
      </Card>
      <Card title="Class advisor">
        <p><strong>{w.advisorName || '–'}</strong></p>
        {w.advisorEmail && <p className="muted"><a href={`mailto:${w.advisorEmail}`}>{w.advisorEmail}</a></p>}
      </Card>
    </>
  );
}
