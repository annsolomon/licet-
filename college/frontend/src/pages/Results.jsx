import React, { useState } from 'react';
import { api } from '../services/api.js';
import { useAction, useApi } from '../hooks/useApi.js';
import { Card, DataTable, ErrorBox, PageHeader, Select, Success } from '../components/ui.jsx';
import { num } from '../utils/format.js';

const COLS = [
  { key: 'studentName', label: 'Student' }, { key: 'subjectCode', label: 'Subject' }, { key: 'semester', label: 'Sem' },
  { key: 'internalMark', label: 'Internal /40', render: (r) => num(r.internalMark) },
  { key: 'semesterMark', label: 'Semester /60', render: (r) => num(r.semesterMark) },
  { key: 'finalMark', label: 'Final /100', render: (r) => num(r.finalMark) },
  { key: 'grade', label: 'Grade' }, { key: 'gradePoint', label: 'GP' }, { key: 'credits', label: 'Cr' },
  { key: 'passFail', label: 'Result', render: (r) => <span className={r.passFail === 'PASS' ? 'good' : 'bad'}>{r.passFail}</span> },
];

export default function Results({ role }) {
  const [semester, setSemester] = useState('');
  const [studentId, setStudentId] = useState('101');
  const students = useApi(() => api.get('/api/students'), []);
  const results = useApi(() => api.get(semester ? `/api/results?semester=${semester}` : '/api/results'), [semester]);
  const summary = useApi(() => (studentId ? api.get(`/api/results/student/${studentId}`) : Promise.resolve(null)), [studentId, results.data]);
  const top = useApi(() => api.get('/api/results/top?limit=5'), [results.data]);
  const calc = useAction();
  const s = summary.data;

  const calcAll = async () => { if (await calc.run(() => api.post('/api/results/calculate-all'))) results.reload(); };

  return (
    <div>
      <PageHeader title="Results, SGPA & CGPA" subtitle="Calculated inside Oracle by PL/SQL procedures and functions"
        flow={['React button', 'POST /api/results/calculate-all', 'ResultController', 'ResultService', 'ResultRepository (CallableStatement)', 'PL/SQL calculate_all_results', 'calculate_internal_mark / semester / grade functions', 'MERGE INTO result']}>
        <select value={semester} onChange={(e) => setSemester(e.target.value)}>
          <option value="">All semesters</option>
          {[1, 2, 3, 4, 5, 6, 7, 8].map((n) => <option key={n} value={n}>Semester {n}</option>)}
        </select>
        {role !== 'STUDENT' && <button onClick={calcAll} disabled={calc.busy}>Calculate all results</button>}
      </PageHeader>
      <ErrorBox error={calc.error || results.error} />
      {calc.result && <Success>{calc.result.calculated} result rows calculated ({calc.result.via}).</Success>}
      <Card title="Student GPA">
        <Select value={studentId} onChange={setStudentId} options={students.data?.map((x) => ({ value: x.id, label: `${x.id} ${x.name}` }))} />
        <ErrorBox error={summary.error} />
        {s && (
          <>
            <p>CGPA (Oracle): <strong>{num(s.cgpa)}</strong> &nbsp; CGPA (Java): <strong>{num(s.javaCgpa)}</strong></p>
            <DataTable columns={[{ key: 'semester', label: 'Semester' }, { key: 'sgpa', label: 'SGPA', render: (r) => num(r.sgpa) }, { key: 'credits', label: 'Credits' }]} rows={s.sgpa} />
            <DataTable columns={COLS} rows={s.results} />
          </>
        )}
      </Card>
      <Card title="Top performers"><DataTable columns={[{ key: 'studentName', label: 'Student' }, { key: 'deptCode', label: 'Dept' }, { key: 'cgpa', label: 'CGPA', render: (r) => num(r.cgpa) }, { key: 'attendancePct', label: 'Attendance %', render: (r) => num(r.attendancePct) }]} rows={top.data?.map((r) => ({ id: r.studentId, ...r }))} /></Card>
      <Card title="All results"><DataTable columns={COLS} rows={results.data} rowClass={(r) => (r.passFail === 'FAIL' ? 'row-bad' : '')} /></Card>
    </div>
  );
}
