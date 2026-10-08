import React, { useState } from 'react';
import { api, qs } from '../services/api.js';
import { useAction, useApi } from '../hooks/useApi.js';
import { Card, Code, DataTable, ErrorBox, Field, PageHeader, Select, Success } from '../components/ui.jsx';
import { num } from '../utils/format.js';

export default function Marks() {
  const students = useApi(() => api.get('/api/students'), []);
  const subjects = useApi(() => api.get('/api/subjects'), []);
  const assessments = useApi(() => api.get('/api/marks/assessments'), []);
  const [studentId, setStudentId] = useState('101');
  const [subjectId, setSubjectId] = useState('2');
  const [code, setCode] = useState('CT1');
  const [raw, setRaw] = useState('');
  const [skip, setSkip] = useState(false);

  const marks = useApi(() => (studentId ? api.get(`/api/marks${qs({ studentId, subjectId })}`) : Promise.resolve([])), [studentId, subjectId]);
  const breakdown = useApi(() => (studentId && subjectId ? api.get(`/api/marks/breakdown${qs({ studentId, subjectId })}`) : Promise.resolve(null)), [studentId, subjectId, marks.data]);
  const save = useAction();

  const submit = async () => {
    const r = await save.run(() => api.post(`/api/marks${skip ? '?skipValidation=true' : ''}`, {
      studentId: Number(studentId), subjectId: Number(subjectId), assessmentCode: code, rawMarks: Number(raw),
    }));
    if (r) marks.reload();
  };
  const max = assessments.data?.find((a) => a.code === code)?.maxMarks;
  const b = breakdown.data;

  return (
    <div>
      <PageHeader title="Internal assessment & marks" subtitle="Raw marks are stored separately; converted marks are calculated"
        flow={['React form', 'POST /api/marks', 'MarksController', 'MarksService (MarkValidator)', 'MarksRepository MERGE', 'marks table', 'Oracle trigger trg_marks_validate (ORA-20001)', 'trg_marks_audit_update -> audit_log']} />
      <Card title="Choose student and subject">
        <div className="form-grid">
          <Field label="Student"><Select value={studentId} onChange={setStudentId} options={students.data?.map((s) => ({ value: s.id, label: `${s.id} ${s.name}` }))} /></Field>
          <Field label="Subject"><Select value={subjectId} onChange={setSubjectId} options={subjects.data?.map((s) => ({ value: s.id, label: `${s.code} ${s.name}` }))} /></Field>
        </div>
      </Card>
      <Card title="Enter / correct one mark">
        <div className="form-grid">
          <Field label="Assessment"><Select value={code} onChange={setCode} options={assessments.data?.map((a) => ({ value: a.code, label: `${a.code} (max ${a.maxMarks})` }))} /></Field>
          <Field label={`Raw marks${max ? ` (0-${max})` : ''}`}><input type="number" step="0.5" value={raw} onChange={(e) => setRaw(e.target.value)} /></Field>
        </div>
        <label className="check"><input type="checkbox" checked={skip} onChange={(e) => setSkip(e.target.checked)} /> skip Java validation (let the Oracle trigger reject marks above the maximum)</label>
        <div className="row"><button onClick={submit} disabled={save.busy || raw === ''}>Save mark</button></div>
        <ErrorBox error={save.error} />
        {save.result && <Success>Saved.</Success>}
      </Card>
      <Card title="Raw marks">
        <ErrorBox error={marks.error} />
        <DataTable columns={[{ key: 'subjectCode', label: 'Subject' }, { key: 'assessmentCode', label: 'Assessment' }, { key: 'rawMarks', label: 'Raw' }, { key: 'maxMarks', label: 'Max' }]} rows={marks.data} />
      </Card>
      <Card title="Step-by-step calculation (Java vs Oracle)">
        <ErrorBox error={breakdown.error} />
        {b && !b.complete && <p className="muted">{b.message}</p>}
        {b && b.complete && (
          <>
            <Code>{b.javaCalc.steps}</Code>
            <DataTable columns={[{ key: 'k', label: '' }, { key: 'java', label: 'Java' }, { key: 'oracle', label: 'Oracle function' }]} rows={[
              { k: 'Internal (40)', java: num(b.javaCalc.internal), oracle: num(b.oracleInternal) },
              { k: 'Semester (60)', java: num(b.javaCalc.semester), oracle: num(b.oracleSemester) },
              { k: 'Final (100)', java: num(b.javaCalc.finalMark), oracle: num(b.oracleFinal) },
            ]} />
            <p>Grade: <strong>{b.grade}</strong> &nbsp; {b.match ? <span className="good">Java and Oracle agree</span> : <span className="bad">MISMATCH</span>}</p>
          </>
        )}
      </Card>
    </div>
  );
}
