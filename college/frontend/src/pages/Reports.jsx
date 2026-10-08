import React, { useState } from 'react';
import { api } from '../services/api.js';
import { useAction, useApi } from '../hooks/useApi.js';
import { Card, Code, DataTable, ErrorBox, PageHeader, Select, Success } from '../components/ui.jsx';

export default function Reports({ role }) {
  const types = useApi(() => api.get('/api/reports/types'), []);
  const files = useApi(() => api.get('/api/reports/files'), []);
  const [type, setType] = useState('STUDENT');
  const [multi, setMulti] = useState({});
  const [preview, setPreview] = useState(null);
  const [ch, setCh] = useState('a');
  const gen = useAction();
  const par = useAction();
  const act = useAction();

  const generate = async () => { if (await gen.run(() => api.post('/api/reports', { type, departmentId: 1, month: '2026-09', threshold: 75 }))) files.reload(); };
  const parallel = async () => {
    const chosen = Object.keys(multi).filter((k) => multi[k]);
    if (await par.run(() => api.post('/api/reports/parallel', { types: chosen }))) files.reload();
  };
  const open = async (name) => { const lines = await act.run(() => api.get(`/api/reports/files/${name}`)); if (lines) setPreview({ name, lines }); };
  const available = (types.data || []).filter((t) => role === 'ADMIN' || t !== 'PAYROLL');

  return (
    <div>
      <PageHeader title="Reports" subtitle="Generate, view, download, copy and analyse text reports (File I/O + multithreading)"
        flow={['React', 'POST /api/reports', 'ReportController', 'ReportService (ReportGenerator interface)', 'Services -> Repositories -> Oracle', 'FileUtil.writeLines (File I/O)', 'output/*.txt', 'ExecutorService for parallel reports']} />
      <Card title="Generate one report">
        <div className="row">
          <Select value={type} onChange={setType} options={available.map((t) => ({ value: t, label: t }))} placeholder="Report type" />
          <button onClick={generate} disabled={gen.busy}>Generate</button>
        </div>
        <ErrorBox error={gen.error} />
        {gen.result && <><Success>{gen.result.name} - {gen.result.characters} characters, {gen.result.lines} lines</Success><Code>{gen.result.preview}</Code></>}
      </Card>
      <Card title="Generate several at the same time (threads)">
        <div className="row wrap">
          {available.map((t) => <label key={t} className="check"><input type="checkbox" checked={!!multi[t]} onChange={(e) => setMulti({ ...multi, [t]: e.target.checked })} /> {t}</label>)}
          <button onClick={parallel} disabled={par.busy}>Generate in parallel</button>
        </div>
        <ErrorBox error={par.error} />
        {par.result && <DataTable columns={[{ key: 'type', label: 'Type' }, { key: 'name', label: 'File' }, { key: 'thread', label: 'Thread' }, { key: 'millis', label: 'ms' }, { key: 'characters', label: 'Chars' }]} rows={par.result.map((r, i) => ({ id: i, ...r }))} />}
      </Card>
      <Card title="Saved report files">
        <div className="row"><span>Count character:</span><input style={{ width: 50 }} maxLength={1} value={ch} onChange={(e) => setCh(e.target.value)} /></div>
        <ErrorBox error={act.error} />
        <DataTable columns={[{ key: 'name', label: 'File' }, { key: 'characters', label: 'Characters' },
          { key: 'a', label: '', render: (r) => (
            <span className="row">
              <button className="small" onClick={() => open(r.name)}>View</button>
              <button className="small secondary" onClick={() => api.download(`/api/reports/files/${r.name}/download`, r.name)}>Download</button>
              <button className="small secondary" onClick={async () => { if (await act.run(() => api.post(`/api/reports/files/${r.name}/copy`))) files.reload(); }}>Copy</button>
              <button className="small secondary" onClick={async () => { const x = await act.run(() => api.get(`/api/reports/files/${r.name}/count?character=${encodeURIComponent(ch)}`)); if (x) window.alert(`"${x.character}" appears ${x.occurrences} times in ${x.totalCharacters} characters`); }}>Count</button>
            </span>) }]}
          rows={files.data?.map((f) => ({ id: f.name, ...f }))} empty="No reports yet - generate one" />
      </Card>
      {preview && <Card title={preview.name} actions={<button className="small secondary" onClick={() => setPreview(null)}>Close</button>}><Code>{preview.lines}</Code></Card>}
    </div>
  );
}
