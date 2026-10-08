import React, { useState } from 'react';
import { useApi } from '@shared/hooks.js';
import { Card, DateField, ErrorNote, PageHead, PctBadge, Progress, SearchField, Spinner, Table } from '@shared/ui.jsx';
import { longDate } from '@shared/format.js';

function Load({ state, children }) {
  if (state.error) return <ErrorNote error={state.error} onRetry={state.reload} />;
  if (!state.data) return <Spinner />;
  return children(state.data);
}
const bar = (r) => (
  <div style={{ minWidth: 120, display: 'grid', gap: 6 }}><PctBadge value={r.percentage ?? r.classAttendance} /><Progress value={r.percentage ?? r.classAttendance} /></div>
);

export function Classes({ api }) {
  const s = useApi(() => api.get('/api/classes'), []);
  return (
    <>
      <PageHead title="Classes" subtitle="Overall attendance and the class advisor" />
      <Card pad={false}><Load state={s}>{(rows) => (
        <Table rows={rows} columns={[
          { key: 'name', label: 'Class' }, { key: 'advisor', label: 'Class advisor' }, { key: 'advisorEmail', label: 'Advisor email' },
          { key: 'students', label: 'Students', align: 'right' }, { key: 'percentage', label: 'Attendance', render: bar },
        ]} />)}</Load></Card>
    </>
  );
}

export function Subjects({ api }) {
  const s = useApi(() => api.get('/api/subjects'), []);
  return (
    <>
      <PageHead title="Subject-wise attendance" subtitle="Attendance of every subject with its teacher" />
      <Card pad={false}><Load state={s}>{(rows) => (
        <Table rowKey="code" rows={rows} columns={[
          { key: 'code', label: 'Code' }, { key: 'name', label: 'Subject' }, { key: 'faculty', label: 'Teacher' },
          { key: 'periodsHeld', label: 'Periods', align: 'right' }, { key: 'absent', label: 'Absent marks', align: 'right' },
          { key: 'percentage', label: 'Attendance', render: bar },
        ]} />)}</Load></Card>
    </>
  );
}

export function Faculty({ api }) {
  const s = useApi(() => api.get('/api/faculty'), []);
  return (
    <>
      <PageHead title="Faculty" subtitle="Who teaches what, and how their classes attend" />
      <Card pad={false}><Load state={s}>{(rows) => (
        <Table rows={rows} columns={[
          { key: 'name', label: 'Name' }, { key: 'designation', label: 'Designation' }, { key: 'subjects', label: 'Subjects' },
          { key: 'advisorOf', label: 'Advisor of' }, { key: 'periodsTaken', label: 'Periods taken', align: 'right' },
          { key: 'classAttendance', label: 'Class attendance', render: bar },
        ]} />)}</Load></Card>
    </>
  );
}

export function AtRisk({ api }) {
  const [threshold, setThreshold] = useState(75);
  const [q, setQ] = useState('');
  const s = useApi(() => api.get(`/api/low?threshold=${threshold}`), [threshold]);
  return (
    <>
      <PageHead title="At-risk students" subtitle={`Overall attendance below ${threshold} %`}>
        <SearchField value={q} onChange={setQ} placeholder="Search name or class" />
        <label className="field inline"><span>Limit</span>
          <select value={threshold} onChange={(e) => setThreshold(Number(e.target.value))}>
            {[65, 70, 75, 80, 85].map((t) => <option key={t} value={t}>{t} %</option>)}
          </select></label>
      </PageHead>
      <Card pad={false}><Load state={s}>{(rows) => (
        <Table empty="Nobody is below this limit 🎉"
          rows={rows.filter((r) => `${r.name} ${r.className}`.toLowerCase().includes(q.toLowerCase()))}
          columns={[
            { key: 'id', label: 'Roll' }, { key: 'name', label: 'Student' }, { key: 'className', label: 'Class' },
            { key: 'absent', label: 'Absent periods', align: 'right' }, { key: 'percentage', label: 'Attendance', render: bar },
            { key: 'parentName', label: 'Parent', render: (r) => (r.parentPhone ? <a href={`tel:${r.parentPhone}`}>{r.parentName} · {r.parentPhone}</a> : '–') },
          ]} />)}</Load></Card>
    </>
  );
}

export function Absentees({ api }) {
  const [date, setDate] = useState('');
  const s = useApi(() => api.get(`/api/absentees${date ? `?date=${date}` : ''}`), [date]);
  return (
    <>
      <PageHead title="Absentees" subtitle={s.data ? longDate(s.data.date) : ''}><DateField value={date || s.data?.date} onChange={setDate} /></PageHead>
      <Card pad={false}><Load state={s}>{(d) => (
        <Table empty="Full attendance on this day." rows={d.students} columns={[
          { key: 'id', label: 'Roll' }, { key: 'name', label: 'Student' }, { key: 'className', label: 'Class' },
          { key: 'absentPeriods', label: 'Periods absent', align: 'right', render: (r) => <span className="badge lvl-bad">{r.absentPeriods}</span> },
          { key: 'periods', label: 'Which periods', render: (r) => r.periods.split(', ').map((p) => `P${p}`).join(' · ') },
          { key: 'parentPhone', label: 'Parent', render: (r) => (r.parentPhone ? <a href={`tel:${r.parentPhone}`}>{r.parentPhone}</a> : '–') },
        ]} />)}</Load></Card>
    </>
  );
}
