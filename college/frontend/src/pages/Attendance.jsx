import React, { useState } from 'react';
import { api, qs } from '../services/api.js';
import { useAction, useApi } from '../hooks/useApi.js';
import { Card, DataTable, ErrorBox, Field, PageHeader, Select, Success } from '../components/ui.jsx';
import { num, today } from '../utils/format.js';

const pctCell = (r) => <span className={r.low ? 'bad' : ''}>{num(r.percentage)}%</span>;
const SUMMARY_COLS = [
  { key: 'studentId', label: 'ID' }, { key: 'studentName', label: 'Student' }, { key: 'subjectCode', label: 'Subject' },
  { key: 'present', label: 'Present' }, { key: 'total', label: 'Total' }, { key: 'percentage', label: '%', render: pctCell },
];

export default function Attendance({ role }) {
  const [view, setView] = useState('all');
  const [target, setTarget] = useState('');
  const classes = useApi(() => api.get('/api/classes'), []);
  const subjects = useApi(() => api.get('/api/subjects'), []);
  const students = useApi(() => api.get('/api/students'), []);
  const depts = useApi(() => api.get('/api/departments'), []);

  const url = {
    all: '/api/attendance/all', low: '/api/attendance/low',
    student: `/api/attendance/student/${target}`, subject: `/api/attendance/subject/${target}`,
    class: `/api/attendance/class/${target}`, department: `/api/attendance/department/${target}`,
  }[view];
  const needsTarget = ['student', 'subject', 'class', 'department'].includes(view);
  const summary = useApi(() => (needsTarget && !target ? Promise.resolve([]) : api.get(url)), [view, target]);

  const targetOptions = {
    student: students.data?.map((s) => ({ value: s.id, label: `${s.id} ${s.name}` })),
    subject: subjects.data?.map((s) => ({ value: s.id, label: `${s.code} ${s.name}` })),
    class: classes.data?.map((c) => ({ value: c.id, label: c.name })),
    department: depts.data?.map((d) => ({ value: d.id, label: `${d.code}` })),
  }[view];

  // ---------- mark a sheet
  const [sheet, setSheet] = useState({ classId: '', subjectId: '', facultyId: '', date: today(), period: 1 });
  const [status, setStatus] = useState({});
  const faculty = useApi(() => api.get('/api/faculty'), []);
  const roster = useApi(() => (sheet.classId ? api.get(`/api/students${qs({ classId: sheet.classId })}`) : Promise.resolve([])), [sheet.classId]);
  const mark = useAction();

  const submit = async () => {
    const records = (roster.data || []).map((s) => ({ studentId: s.id, status: status[s.id] || 'PRESENT' }));
    const res = await mark.run(() => api.post('/api/attendance', {
      classId: Number(sheet.classId), subjectId: Number(sheet.subjectId), facultyId: Number(sheet.facultyId),
      date: sheet.date, period: Number(sheet.period), records,
    }));
    if (res) summary.reload();
  };

  return (
    <div>
      <PageHeader title="Attendance" subtitle="Mark a class period and view attendance by student, subject, class or department"
        flow={['React sheet', 'POST /api/attendance', 'AttendanceController', 'AttendanceService (validate class/subject/faculty)', 'AttendanceRepository', 'JdbcTemplate batchUpdate (MERGE)', 'attendance_session + attendance_record', 'Oracle', 'calculate_attendance()']} />
      <Card title="View attendance">
        <div className="row">
          <select value={view} onChange={(e) => { setView(e.target.value); setTarget(''); }}>
            <option value="all">All students</option><option value="low">Low attendance (&lt; 75 %)</option>
            <option value="student">By student</option><option value="subject">By subject</option>
            <option value="class">By class</option><option value="department">By department</option>
          </select>
          {needsTarget && <Select value={target} onChange={setTarget} options={targetOptions} />}
        </div>
        <ErrorBox error={summary.error} />
        <DataTable columns={SUMMARY_COLS} rows={summary.data} rowClass={(r) => (r.low ? 'row-bad' : '')} />
      </Card>
      {role !== 'STUDENT' && (
        <Card title="Mark attendance">
          <div className="form-grid">
            <Field label="Class"><Select value={sheet.classId} onChange={(v) => setSheet({ ...sheet, classId: v })} options={classes.data?.map((c) => ({ value: c.id, label: c.name }))} /></Field>
            <Field label="Subject"><Select value={sheet.subjectId} onChange={(v) => setSheet({ ...sheet, subjectId: v })} options={subjects.data?.map((s) => ({ value: s.id, label: `${s.code} ${s.name}` }))} /></Field>
            <Field label="Faculty"><Select value={sheet.facultyId} onChange={(v) => setSheet({ ...sheet, facultyId: v })} options={faculty.data?.map((f) => ({ value: f.id, label: f.name }))} /></Field>
            <Field label="Date"><input type="date" value={sheet.date} onChange={(e) => setSheet({ ...sheet, date: e.target.value })} /></Field>
            <Field label="Period (1-8)"><input type="number" min="1" max="8" value={sheet.period} onChange={(e) => setSheet({ ...sheet, period: e.target.value })} /></Field>
          </div>
          {(roster.data || []).length > 0 && (
            <DataTable columns={[
              { key: 'id', label: 'ID' }, { key: 'name', label: 'Student' },
              { key: 's', label: 'Status', render: (s) => (
                <select value={status[s.id] || 'PRESENT'} onChange={(e) => setStatus({ ...status, [s.id]: e.target.value })}>
                  <option>PRESENT</option><option>ABSENT</option><option>OD</option><option>MEDICAL</option><option>OTHER</option>
                </select>) },
            ]} rows={roster.data} />
          )}
          <div className="row">
            <button onClick={submit} disabled={mark.busy || !sheet.classId || !sheet.subjectId || !sheet.facultyId}>Save attendance</button>
          </div>
          <ErrorBox error={mark.error} />
          {mark.result && <Success>Saved: {JSON.stringify(mark.result)}</Success>}
        </Card>
      )}
    </div>
  );
}
