import React, { useState } from 'react';
import { useApi } from '@shared/hooks.js';
import { Card, ErrorNote, PageHead, SearchField, Spinner } from '@shared/ui.jsx';
import { InternalsSummary } from '@shared/people.jsx';
import { StudentDetail } from '@shared/ward.jsx';
import { level } from '@shared/format.js';

/** students x subjects grid of the internal marks /40; red = below the pass mark */
export default function InternalsGrid({ api }) {
  const [q, setQ] = useState('');
  const [open, setOpen] = useState(null);
  const { data, error, reload } = useApi(() => api.get('/api/internals/grid'), []);
  if (error) return <ErrorNote error={error} onRetry={reload} />;
  if (!data) return <Spinner />;
  const rows = data.students.filter((s) => `${s.name} ${s.id}`.toLowerCase().includes(q.toLowerCase()));
  const avg = (s) => {
    const v = data.subjects.map((c) => s.marks[c]).filter((x) => x != null);
    return v.length ? Math.round((v.reduce((a, b) => a + Number(b), 0) / v.length) * 10) / 10 : null;
  };
  const cls = (v) => (v == null ? 'st-NONE' : `lvl-${level((Number(v) / 40) * 100, 50)} cellmark`);
  return (
    <>
      <PageHead title="Internal marks" subtitle={`Out of 40 · pass mark ${data.passMark} · click a student for the full breakdown`}>
        <SearchField value={q} onChange={setQ} placeholder="Search student" />
      </PageHead>
      <Card pad={false}>
        <div className="grid-wrap">
          <table className="agrid">
            <thead><tr><th>Student</th>{data.subjects.map((c) => <th key={c}>{c}</th>)}<th>Avg</th></tr></thead>
            <tbody>
              {rows.map((s) => (
                <tr key={s.id} className="click" onClick={() => setOpen(s.id)}>
                  <td><strong>{s.name}</strong> <span className="muted small">#{s.id}</span></td>
                  {data.subjects.map((c) => (
                    <td key={c} style={{ textAlign: 'center' }}><span className={`mk ${cls(s.marks[c])}`}>{s.marks[c] ?? '–'}</span></td>
                  ))}
                  <td style={{ textAlign: 'center' }}><strong>{avg(s) ?? '–'}</strong></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
      <InternalsSummary api={api} />
      {open && <StudentDetail api={api} id={open} onClose={() => setOpen(null)} />}
    </>
  );
}
