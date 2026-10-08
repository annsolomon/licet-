import React, { useState } from 'react';
import { useApi } from '@shared/hooks.js';
import { Card, Empty, ErrorNote, PctBadge, Progress, Spinner } from '@shared/ui.jsx';
import { longDate } from '@shared/format.js';
import { PeriodList } from './Home.jsx';

export function Subjects({ api }) {
  const { data, error, reload } = useApi(() => api.get('/api/subjects'), []);
  if (error) return <ErrorNote error={error} onRetry={reload} />;
  if (!data) return <Spinner />;
  return (
    <Card title="Subject-wise attendance" subtitle="The line on each bar marks the 75 % limit">
      {data.length === 0 && <Empty>No attendance yet.</Empty>}
      {data.map((s) => (
        <div key={s.code} className="subject">
          <div className="subject-top"><div><strong>{s.name}</strong><div className="muted small">{s.code} · {s.faculty}</div></div><PctBadge value={s.percentage} /></div>
          <Progress value={s.percentage} />
          <div className="small muted">
            {s.present} of {s.total} periods attended ·{' '}
            {s.low ? <strong style={{ color: 'var(--bad)' }}>needs {s.mustAttend} more in a row to reach 75 %</strong> : <>can miss {s.canMiss} more and stay above 75 %</>}
          </div>
        </div>
      ))}
    </Card>
  );
}

export function History({ api }) {
  const { data, error, reload } = useApi(() => api.get('/api/history?days=30'), []);
  const [open, setOpen] = useState(null);
  const day = useApi(() => (open ? api.get(`/api/day?date=${open}`) : Promise.resolve(null)), [open]);
  if (error) return <ErrorNote error={error} onRetry={reload} />;
  if (!data) return <Spinner />;
  return (
    <div className="hist">
      <h3>Last 30 days</h3>
      {data.length === 0 && <Empty>No attendance yet.</Empty>}
      {data.map((d) => (
        <div key={d.date} className="hist-row">
          <button onClick={() => setOpen(open === d.date ? null : d.date)} aria-expanded={open === d.date}>
            <span><strong>{longDate(d.date)}</strong><br />
              <span className="muted small">{d.absent ? `Absent: ${d.absentPeriods.split(', ').map((p) => `P${p}`).join(', ')}` : 'Present all day'}</span></span>
            <span className={`badge ${d.absent ? 'lvl-bad' : 'lvl-good'}`}>{d.present}/{d.periods}</span>
          </button>
          {open === d.date && <div className="hist-detail">{day.data ? <PeriodList periods={day.data.periods} /> : <Spinner />}</div>}
        </div>
      ))}
    </div>
  );
}
