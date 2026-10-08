import React, { useState } from 'react';
import { api } from '../services/api.js';
import { useAction, useApi } from '../hooks/useApi.js';
import { Card, Code, ErrorBox, PageHeader } from '../components/ui.jsx';

function StepResult({ step }) {
  return (
    <div className={`step ${step.error ? 'step-bad' : ''}`}>
      <div className="step-head"><span className="tag">{step.kind}</span> <span className="muted">{step.note}</span></div>
      <Code>{step.sql}</Code>
      {step.columns.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead><tr>{step.columns.map((c) => <th key={c}>{c}</th>)}</tr></thead>
            <tbody>{step.rows.map((r, i) => <tr key={i}>{r.map((v, j) => <td key={j}>{v}</td>)}</tr>)}</tbody>
          </table>
        </div>
      )}
      <div className={step.error ? 'bad' : 'muted'}>{step.message}</div>
    </div>
  );
}

export default function DbLab() {
  const topics = useApi(() => api.get('/api/dblab/topics'), []);
  const [current, setCurrent] = useState(null);
  const run = useAction();

  const choose = (t) => { setCurrent(t); run.clear(); };
  const execute = () => run.run(() => api.post(`/api/dblab/topics/${current.number}/run`));

  return (
    <div>
      <PageHeader title="DBMS Lab" subtitle="32 DBMS syllabus topics run on the real project tables (every demo rolls back)"
        flow={['React', 'POST /api/dblab/topics/N/run', 'DbLabController', 'DbmsDemoService', 'JDBC connection (auto-commit off)', 'real SQL / PL-SQL', 'Oracle', 'ROLLBACK']} />
      <ErrorBox error={topics.error} />
      <div className="lab">
        <div className="lab-list">
          {(topics.data || []).map((t) => (
            <button key={t.id} className={`topic ${current?.id === t.id ? 'active' : ''}`} onClick={() => choose(t)}>
              <span className="num">{t.number}</span> {t.title}
            </button>
          ))}
        </div>
        <div className="lab-main">
          {!current && <p className="muted">Choose a topic on the left.</p>}
          {current && (
            <>
              <Card title={`${current.number}. ${current.title}`} actions={<button onClick={execute} disabled={run.busy}>{run.busy ? 'Running...' : 'Run demo'}</button>}>
                <p><strong>Category:</strong> {current.category} &nbsp; <strong>File:</strong> <code>{current.file}</code></p>
                <p><strong>In this project:</strong> {current.projectUse}</p>
                <p><strong>Idea:</strong> {current.explanation}</p>
                <p><strong>Say to the evaluator:</strong> {current.evaluatorSay}</p>
                <p><strong>Viva:</strong> {current.viva}</p>
                <p><strong>Expected:</strong> {current.expected}</p>
              </Card>
              <ErrorBox error={run.error} />
              {run.result && run.result.map((s, i) => <StepResult key={i} step={s} />)}
              {!run.result && <Card title="SQL that will run"><Code>{current.statements.join(';\n\n') + ';'}</Code></Card>}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
