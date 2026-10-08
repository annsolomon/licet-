import React, { useState } from 'react';
import { api, qs } from '../services/api.js';
import { useAction, useApi } from '../hooks/useApi.js';
import { Card, Code, ErrorBox, Field, PageHeader } from '../components/ui.jsx';

function Tool({ title, fields, call, initial }) {
  const [vals, setVals] = useState(initial);
  const act = useAction();
  return (
    <Card title={title}>
      <div className="row wrap">
        {fields.map((f) => (
          <Field key={f.name} label={f.label}>
            <input value={vals[f.name]} onChange={(e) => setVals({ ...vals, [f.name]: e.target.value })} />
          </Field>
        ))}
        <button onClick={() => act.run(() => api.get(call(vals)))} disabled={act.busy}>Run</button>
      </div>
      <ErrorBox error={act.error} />
      {act.result && <Code>{JSON.stringify(act.result, null, 2)}</Code>}
    </Card>
  );
}

export default function JavaLab() {
  const all = useApi(() => api.get('/api/lab/all'), []);
  return (
    <div>
      <PageHeader title="Java Lab" subtitle="All 15 Java syllabus topics, run by the Spring Boot server"
        flow={['React', 'GET /api/lab/all', 'LabController', 'SyllabusLabService', 'SyllabusDemos (plain Java)', 'output lines', 'JSON', 'this page']} />
      <div className="two">
        <Tool title="Factorial" initial={{ n: '5' }} fields={[{ name: 'n', label: 'n (0-20)' }]} call={(v) => `/api/lab/factorial${qs(v)}`} />
        <Tool title="Fibonacci" initial={{ count: '10' }} fields={[{ name: 'count', label: 'How many terms' }]} call={(v) => `/api/lab/fibonacci${qs(v)}`} />
        <Tool title="Sorting" initial={{ numbers: '5,3,9,1,7' }} fields={[{ name: 'numbers', label: 'Numbers' }]} call={(v) => `/api/lab/sort${qs(v)}`} />
        <Tool title="Binary search" initial={{ numbers: '5,3,9,1,7', key: '7' }} fields={[{ name: 'numbers', label: 'Numbers' }, { name: 'key', label: 'Key' }]} call={(v) => `/api/lab/binary-search${qs(v)}`} />
        <Tool title="Shapes (abstract class)" initial={{ type: 'rectangle', a: '4', b: '5' }} fields={[{ name: 'type', label: 'rectangle | triangle | circle' }, { name: 'a', label: 'a' }, { name: 'b', label: 'b' }]} call={(v) => `/api/lab/shape${qs(v)}`} />
        <Tool title="String handling" initial={{ text: 'Madam' }} fields={[{ name: 'text', label: 'Text' }]} call={(v) => `/api/lab/string${qs(v)}`} />
        <Tool title="Generic search on students" initial={{ keyword: 'ra' }} fields={[{ name: 'keyword', label: 'Keyword' }]} call={(v) => `/api/lab/student-search${qs(v)}`} />
        <Tool title="Plain JDBC vs JdbcTemplate" initial={{ deptId: '1' }} fields={[{ name: 'deptId', label: 'Department id' }]} call={(v) => `/api/lab/jdbc-compare${qs(v)}`} />
      </div>
      <ErrorBox error={all.error} />
      {all.data && Object.entries(all.data).map(([title, lines]) => (
        <Card key={title} title={title}><Code>{lines}</Code></Card>
      ))}
    </div>
  );
}
