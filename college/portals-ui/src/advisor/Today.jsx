import React, { useEffect, useState } from 'react';
import { useAction, useApi } from '@shared/hooks.js';
import { Card, DateField, ErrorNote, PageHead, SearchField, Spinner } from '@shared/ui.jsx';
import { STATUS_LABEL, STATUS_SHORT, longDate } from '@shared/format.js';

const NEXT = { PRESENT: 'ABSENT', ABSENT: 'PRESENT', OD: 'PRESENT', MEDICAL: 'PRESENT', OTHER: 'PRESENT' };

export default function Today({ api, onMarked }) {
  const [date, setDate] = useState('');
  const [q, setQ] = useState('');
  const [onlyAbsent, setOnlyAbsent] = useState(false);
  const [mark, setMark] = useState(false);
  const { data, error, loading, reload } = useApi(() => api.get(`/api/day${date ? `?date=${date}` : ''}`), [date]);
  const act = useAction();
  const [local, setLocal] = useState({});

  if (error) return <ErrorNote error={error} onRetry={reload} />;
  if (!data) return <Spinner />;

  const toggle = async (cell) => {
    if (!cell.recordId) return;
    const status = NEXT[local[cell.recordId] || cell.status];
    const ok = await act.run(() => api.put(`/api/records/${cell.recordId}`, { status }));
    if (ok) { setLocal((l) => ({ ...l, [cell.recordId]: status })); onMarked?.(); }
  };
  const statusOf = (c) => local[c.recordId] || c.status;
  const students = data.students
    .filter((s) => s.name.toLowerCase().includes(q.toLowerCase()) || String(s.id).includes(q))
    .filter((s) => !onlyAbsent || s.cells.some((c) => statusOf(c) === 'ABSENT'));
  const held = data.periods.filter((p) => p.held).length;

  return (
    <>
      <PageHead title="Periods" subtitle={`${longDate(data.date)} · ${held} of 8 periods taken`}>
        <SearchField value={q} onChange={setQ} placeholder="Search student" />
        <DateField value={date || data.date} onChange={(d) => { setDate(d); setLocal({}); }} />
        <button className="btn btn-primary" onClick={() => setMark(true)}>Mark a period</button>
      </PageHead>
      <ErrorNote error={act.error} />
      <div className="legend">
        <span><i className="st-PRESENT" />Present</span><span><i className="st-ABSENT" />Absent</span>
        <span><i className="st-OD" />On duty</span><span><i className="st-MEDICAL" />Medical / other</span>
        <label className="row"><input type="checkbox" checked={onlyAbsent} onChange={(e) => setOnlyAbsent(e.target.checked)} /> Only students with absences</label>
      </div>
      <Card pad={false} title={undefined}>
        {loading && <Spinner />}
        <div className="grid-wrap">
          <table className="agrid">
            <thead>
              <tr>
                <th>Student</th>
                {data.periods.map((p) => (
                  <th key={p.period} className="period-head">P{p.period}<small>{p.held ? p.code : 'not held'}</small><small>{p.held ? `${p.absent} abs` : ''}</small></th>
                ))}
                <th>Abs</th>
              </tr>
            </thead>
            <tbody>
              {students.map((s) => (
                <tr key={s.id}>
                  <td><strong>{s.name}</strong> <span className="muted small">#{s.id}</span></td>
                  {data.periods.map((p) => {
                    const c = s.cells.find((x) => x.period === p.period);
                    const st = c ? statusOf(c) : null;
                    return (
                      <td key={p.period} style={{ textAlign: 'center' }}>
                        <button className={`cell st-${st || 'NONE'}`} disabled={!c || !c.recordId || act.busy} onClick={() => c && toggle(c)}
                          title={st ? `${STATUS_LABEL[st]} – click to change` : 'Not marked'} aria-label={`${s.name} period ${p.period} ${st ? STATUS_LABEL[st] : 'not marked'}`}>
                          {st ? STATUS_SHORT[st] : '·'}
                        </button>
                      </td>
                    );
                  })}
                  <td style={{ textAlign: 'center' }}>{s.cells.filter((c) => statusOf(c) === 'ABSENT').length}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
      <p className="muted small">Click a box to flip Present ↔ Absent. Marking a student absent alerts the parent and you straight away.</p>
      {mark && <MarkPanel api={api} latest={data.date} onClose={() => setMark(false)} onDone={(d) => { setMark(false); setLocal({}); setDate(d); reload(); onMarked?.(); }} />}
    </>
  );
}

function nextWeekday(iso) {
  const d = new Date(`${iso}T00:00:00`);
  do { d.setDate(d.getDate() + 1); } while (d.getDay() === 0 || d.getDay() === 6);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function MarkPanel({ api, latest, onClose, onDone }) {
  const [date, setDate] = useState(() => nextWeekday(latest));
  const [period, setPeriod] = useState(1);
  const [absent, setAbsent] = useState(new Set());
  const [done, setDone] = useState(null);
  const day = useApi(() => api.get(`/api/day?date=${date}`), [date]);
  const act = useAction();
  // show what is already saved for this period, so a correction starts from the current state
  useEffect(() => {
    if (!day.data) return;
    setAbsent(new Set(day.data.students.filter((s) => s.cells.some((c) => c.period === period && c.status === 'ABSENT')).map((s) => s.id)));
  }, [day.data, period]);
  const flip = (id) => setAbsent((s) => { const n = new Set(s); n.has(id) ? n.delete(id) : n.add(id); return n; });
  const students = day.data?.students || [];
  const submit = async () => {
    const r = await act.run(() => api.post('/api/period/mark', { date, period, absentIds: [...absent] }));
    if (r) setDone(r);
  };
  return (
    <div onClick={onClose} style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,.45)', display: 'grid', placeItems: 'center', padding: 16, zIndex: 50 }}>
      <div onClick={(e) => e.stopPropagation()} style={{ width: 'min(760px,100%)', maxHeight: '90vh', overflow: 'auto' }}>
        <Card title="Mark a period" subtitle="Tick the students who are absent. Everyone else is marked present."
          actions={<button className="btn btn-quiet" onClick={onClose}>Close</button>}>
          {done ? (
            <>
              <div className="note note-good" role="status">
                <span><strong>Saved.</strong> Period {done.period} on {longDate(done.date)}: {done.present} present, {done.absent} absent.
                  {done.absent > 0 ? ` ${done.absent} parent SMS and one absentee list for you were queued.` : ''}</span>
              </div>
              <div className="row" style={{ marginTop: 14, justifyContent: 'flex-end' }}>
                <button className="btn" onClick={() => setDone(null)}>Mark another period</button>
                <button className="btn btn-primary" onClick={() => onDone(done.date)}>Show the grid</button>
              </div>
            </>
          ) : (
            <>
              <div className="row" style={{ marginBottom: 12 }}>
                <DateField value={date} onChange={setDate} />
                <label className="field inline"><span>Period</span>
                  <select value={period} onChange={(e) => setPeriod(Number(e.target.value))}>
                    {[1, 2, 3, 4, 5, 6, 7, 8].map((p) => {
                      const x = day.data?.periods.find((q) => q.period === p);
                      return <option key={p} value={p}>P{p}{x?.startTime ? ` · ${x.startTime}–${x.endTime}` : ''}{x?.code ? ` · ${x.code}` : ''}</option>;
                    })}
                  </select></label>
              </div>
              <p className="muted small" style={{ marginBottom: 10 }}>The subject and teacher come from the timetable.</p>
              {day.loading && !day.data && <Spinner />}
              <ErrorNote error={day.error} onRetry={day.reload} />
              <div className="mark-list">
                {students.map((s) => (
                  <label key={s.id} className={`mark-item ${absent.has(s.id) ? 'on' : ''}`}>
                    <input type="checkbox" checked={absent.has(s.id)} onChange={() => flip(s.id)} /> {s.name} <span className="muted small">#{s.id}</span>
                  </label>
                ))}
              </div>
              <ErrorNote error={act.error} />
              <div className="row" style={{ marginTop: 14, justifyContent: 'space-between' }}>
                <span className="muted">{absent.size} absent · {students.length - absent.size} present</span>
                <button className="btn btn-primary" disabled={act.busy || !students.length} onClick={submit}>{act.busy ? 'Saving…' : 'Save & send alerts'}</button>
              </div>
            </>
          )}
        </Card>
      </div>
    </div>
  );
}
