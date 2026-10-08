import React, { useState } from 'react';
import { useApi } from '@shared/hooks.js';
import { Bars, LineChart } from '@shared/charts.jsx';
import { Card, DateField, Empty, ErrorNote, Kpi, PageHead, Spinner } from '@shared/ui.jsx';
import { level, longDate, pct, shortDate } from '@shared/format.js';

export default function Overview({ api, go }) {
  const [date, setDate] = useState('');
  const { data, error, loading, reload } = useApi(() => api.get(`/api/overview${date ? `?date=${date}` : ''}`), [date]);
  const notif = useApi(() => api.get('/api/notifications/summary'), []);

  if (error) return <ErrorNote error={error} onRetry={reload} />;
  if (!data) return <Spinner />;
  const { department: d, day, periods, trend } = data;
  const tone = { good: 'good', ok: 'ok', bad: 'bad', none: 'neutral' }[level(day.percentage)];
  const sum = (recipient, status) => (notif.data || []).filter((n) => n.recipient === recipient && (!status || n.status === status)).reduce((a, n) => a + Number(n.count), 0);

  return (
    <>
      <PageHead title={`${d.name}`} subtitle={`${longDate(data.date)} · ${d.students} students · ${d.classes} class${d.classes === 1 ? '' : 'es'}`}>
        <DateField value={date || data.date} onChange={setDate} />
      </PageHead>
      {loading && <Spinner />}
      <div className="kpis">
        <Kpi label="Attendance" value={pct(day.percentage)} tone={tone} hint={`${day.present} of ${day.records} period marks`} />
        <Kpi label="Absent period marks" value={day.absent} tone={day.absent ? 'bad' : 'good'} hint={`${day.studentsAbsent} students`} />
        <Kpi label="Below 75 %" value={data.lowCount} tone={data.lowCount ? 'bad' : 'good'} hint="overall attendance" />
        <Kpi label="Parent alerts" value={sum('PARENT')} hint={`${sum('PARENT', 'SENT')} sent · ${sum('PARENT', 'FAILED')} failed`} />
        <Kpi label="Advisor alerts" value={sum('ADVISOR')} hint={`${sum('ADVISOR', 'SENT')} sent · ${sum('ADVISOR', 'PENDING')} pending`} />
      </div>
      <div className="grid g2">
        <Card title="Period by period" subtitle="Attendance in each of the 8 periods">
          {periods.every((p) => p.percentage == null) ? <Empty>No periods marked on this day.</Empty>
            : <Bars items={periods.map((p) => ({ label: `P${p.period}`, value: p.percentage }))} />}
        </Card>
        <Card title="Last 14 days" subtitle="Daily attendance of the department">
          <LineChart points={trend.map((t) => ({ label: shortDate(t.date), value: t.percentage }))} />
        </Card>
      </div>
      <Card title="Today's absentees" actions={<button className="btn" onClick={() => go('absentees')}>Open list</button>}>
        <p className="muted">{day.studentsAbsent} students were absent in at least one period. Parents and class advisors were alerted automatically.</p>
      </Card>
    </>
  );
}
