import React, { useState } from 'react';
import { api } from '../services/api.js';
import { useAction, useApi } from '../hooks/useApi.js';
import { Card, DataTable, ErrorBox, PageHeader, Success } from '../components/ui.jsx';

export default function Grades({ role }) {
  const grades = useApi(() => api.get('/api/grades'), []);
  const [edit, setEdit] = useState(null);
  const save = useAction();
  const submit = async () => {
    const ok = await save.run(() => api.put(`/api/grades/${edit.code}`, {
      minMark: Number(edit.minMark), maxMark: Number(edit.maxMark), gradePoint: Number(edit.gradePoint), pass: edit.pass,
    }));
    if (ok) { setEdit(null); grades.reload(); }
  };
  const cols = [{ key: 'code', label: 'Grade' }, { key: 'minMark', label: 'Min' }, { key: 'maxMark', label: 'Max' },
    { key: 'gradePoint', label: 'Grade point' }, { key: 'pass', label: 'Pass?', render: (r) => (r.pass ? 'Yes' : 'No') }];
  if (role === 'ADMIN') cols.push({ key: 'a', label: '', render: (r) => <button className="small" onClick={() => setEdit({ ...r })}>Edit</button> });
  return (
    <div>
      <PageHeader title="Grades" subtitle="The grade scale is data in table GRADE, not hard-coded"
        flow={['React', 'GET/PUT /api/grades', 'ResultController', 'ResultService (range checks)', 'GradeRepository', 'JdbcTemplate', 'grade table', 'calculate_grade() reads it']} />
      <ErrorBox error={grades.error} />
      {edit && (
        <Card title={`Edit grade ${edit.code}`}>
          <div className="form-grid">
            {['minMark', 'maxMark', 'gradePoint'].map((k) => (
              <label key={k} className="field"><span>{k}</span><input type="number" value={edit[k]} onChange={(e) => setEdit({ ...edit, [k]: e.target.value })} /></label>
            ))}
            <label className="check"><input type="checkbox" checked={edit.pass} onChange={(e) => setEdit({ ...edit, pass: e.target.checked })} /> pass grade</label>
          </div>
          <div className="row"><button onClick={submit}>Save</button><button className="secondary" onClick={() => setEdit(null)}>Cancel</button></div>
          <ErrorBox error={save.error} />
        </Card>
      )}
      {save.result && <Success>Grade updated. Run "Calculate all results" to apply it.</Success>}
      <DataTable columns={cols} rows={grades.data?.map((g) => ({ ...g, id: g.code }))} />
    </div>
  );
}
