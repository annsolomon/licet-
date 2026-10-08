import React, { useState } from 'react';
import { useApi } from '@shared/hooks.js';
import { Card, ErrorNote, PageHead, PctBadge, Progress, SearchField, Spinner, Table } from '@shared/ui.jsx';

function Load({ state, children }) {
  if (state.error) return <ErrorNote error={state.error} onRetry={state.reload} />;
  if (!state.data) return <Spinner />;
  return children(state.data);
}
const bar = (r) => <div style={{ minWidth: 120, display: 'grid', gap: 6 }}><PctBadge value={r.percentage} /><Progress value={r.percentage} /></div>;
const phone = (r) => (r.parentPhone ? <a href={`tel:${r.parentPhone}`}>{r.parentName} · {r.parentPhone}</a> : '–');

export function Students({ api }) {
  const [q, setQ] = useState('');
  const s = useApi(() => api.get('/api/students'), []);
  return (
    <>
      <PageHead title="Students" subtitle="Overall attendance and parent contact"><SearchField value={q} onChange={setQ} placeholder="Search student" /></PageHead>
      <Card pad={false}><Load state={s}>{(rows) => (
        <Table rows={rows.filter((r) => r.name.toLowerCase().includes(q.toLowerCase()) || String(r.id).includes(q))} columns={[
          { key: 'id', label: 'Roll' }, { key: 'name', label: 'Student' },
          { key: 'absentToday', label: 'Absent (latest day)', align: 'right', render: (r) => (r.absentToday ? <span className="badge lvl-bad">{r.absentToday} periods</span> : '–') },
          { key: 'percentage', label: 'Overall', render: bar }, { key: 'parentName', label: 'Parent', render: phone },
        ]} />)}</Load></Card>
    </>
  );
}

export function Subjects({ api }) {
  const s = useApi(() => api.get('/api/subjects'), []);
  return (
    <>
      <PageHead title="Subject-wise attendance" subtitle="How your class attends each subject" />
      <Card pad={false}><Load state={s}>{(rows) => (
        <Table rowKey="code" rows={rows} columns={[
          { key: 'code', label: 'Code' }, { key: 'name', label: 'Subject' }, { key: 'faculty', label: 'Teacher' },
          { key: 'periodsHeld', label: 'Periods', align: 'right' }, { key: 'absent', label: 'Absent marks', align: 'right' }, { key: 'percentage', label: 'Attendance', render: bar },
        ]} />)}</Load></Card>
    </>
  );
}

export function AtRisk({ api }) {
  const s = useApi(() => api.get('/api/low'), []);
  return (
    <>
      <PageHead title="At-risk students" subtitle="Overall attendance below 75 %" />
      <Card pad={false}><Load state={s}>{(rows) => (
        <Table empty="Nobody is below 75 % 🎉" rows={rows} columns={[
          { key: 'id', label: 'Roll' }, { key: 'name', label: 'Student' }, { key: 'absent', label: 'Absent periods', align: 'right' },
          { key: 'percentage', label: 'Attendance', render: bar }, { key: 'parentName', label: 'Parent', render: phone },
        ]} />)}</Load></Card>
    </>
  );
}
