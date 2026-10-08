import React, { useState } from 'react';
import { useApi } from './hooks.js';
import { Card, ErrorNote, PageHead, PctBadge, Progress, SearchField, Spinner, Table } from './ui.jsx';
import { level } from './format.js';
import { StudentDetail } from './ward.jsx';

const bar = (v) => <div style={{ minWidth: 110, display: 'grid', gap: 6 }}><PctBadge value={v} /><Progress value={v} /></div>;
export const internalBadge = (avg) => <span className={`badge lvl-${level(avg == null ? null : (Number(avg) / 40) * 100, 50)}`}>{avg == null ? '–' : `${avg} / 40`}</span>;

/** Students list with attendance and internals; a click opens the full picture of one student. */
export function StudentsPage({ api, title = 'Students', subtitle, extra = [], withClass = true }) {
  const [q, setQ] = useState('');
  const [filter, setFilter] = useState('all');
  const [open, setOpen] = useState(null);
  const s = useApi(() => api.get(`/api${api.overviewPath}`), []);
  const extraData = useApi(() => (extra.length ? api.get('/api/students') : Promise.resolve([])), []);
  if (s.error) return <ErrorNote error={s.error} onRetry={s.reload} />;
  if (!s.data) return <Spinner />;
  const extraById = Object.fromEntries((extraData.data || []).map((r) => [r.id, r]));
  const rows = s.data.map((r) => ({ ...extraById[r.id], ...r }))
    .filter((r) => `${r.name} ${r.className} ${r.id}`.toLowerCase().includes(q.toLowerCase()))
    .filter((r) => filter === 'all' || (filter === 'att' && Number(r.attendance) < 75) || (filter === 'int' && Number(r.below) > 0));
  const cols = [
    { key: 'id', label: 'Roll' }, { key: 'name', label: 'Student' },
    ...(withClass ? [{ key: 'className', label: 'Class' }] : []),
    { key: 'attendance', label: 'Attendance', render: (r) => bar(r.attendance) },
    { key: 'internalAvg', label: 'Internal avg', render: (r) => internalBadge(r.internalAvg) },
    { key: 'below', label: 'Subjects below pass', align: 'right', render: (r) => (Number(r.below) ? <span className="badge lvl-bad">{r.below}</span> : '–') },
    ...extra,
  ];
  return (
    <>
      <PageHead title={title} subtitle={subtitle || 'Click a student for attendance by subject and internal marks'}>
        <SearchField value={q} onChange={setQ} placeholder="Search name or roll" />
        <select value={filter} onChange={(e) => setFilter(e.target.value)} aria-label="Filter">
          <option value="all">All students</option>
          <option value="att">Attendance below 75 %</option>
          <option value="int">Internals below pass</option>
        </select>
      </PageHead>
      <Card pad={false}><Table rows={rows} columns={cols} onRow={(r) => setOpen(r.id)} empty="No student matches" /></Card>
      {open && <StudentDetail api={api} id={open} onClose={() => setOpen(null)} />}
    </>
  );
}

/** Class x subject summary of the internal marks (HOD: whole department, advisor: own class). */
export function InternalsSummary({ api }) {
  const s = useApi(() => api.get('/api/internals/subjects'), []);
  if (s.error) return <ErrorNote error={s.error} onRetry={s.reload} />;
  if (!s.data) return <Spinner />;
  return (
    <Card title="Subject-wise internal marks" subtitle="Average, lowest and highest internal mark out of 40; pass mark 20" pad={false}>
      <Table rowKey="code" rows={s.data} columns={[
        { key: 'className', label: 'Class' }, { key: 'code', label: 'Code' }, { key: 'name', label: 'Subject' }, { key: 'faculty', label: 'Teacher' },
        { key: 'average', label: 'Average', render: (r) => internalBadge(r.average) },
        { key: 'lowest', label: 'Lowest', align: 'right' }, { key: 'highest', label: 'Highest', align: 'right' },
        { key: 'below', label: 'Below pass', align: 'right', render: (r) => (Number(r.below) ? <span className="badge lvl-bad">{r.below} of {r.students}</span> : '–') },
      ]} />
    </Card>
  );
}
