import React, { useState } from 'react';
import { useApi } from './hooks.js';
import { Ring } from './charts.jsx';
import { Card, Empty, ErrorNote, Inbox, PctBadge, Progress, Spinner, useUnread } from './ui.jsx';
import { STATUS_LABEL, STATUS_SHORT, level, longDate, pct } from './format.js';

/* ---------- screens shared by the Parent portal and the Student portal (same API, different login) ---------- */

export function PeriodStrip({ periods }) {
  return (
    <div className="strip">
      {periods.map((p) => (
        <div key={p.period} className={`chip st-${p.status || 'NONE'}`} title={`${p.subject || 'No class'} · ${p.status ? STATUS_LABEL[p.status] : 'not marked'}`}>
          {p.status ? STATUS_SHORT[p.status] : '·'}<small>P{p.period}</small>
        </div>
      ))}
    </div>
  );
}

export function PeriodList({ periods }) {
  return (
    <div className="plist">
      {periods.map((p) => (
        <div key={p.period} className="prow">
          <span className="pn">P{p.period}</span>
          <span><strong>{p.subject || 'No class'}</strong><br /><span className="muted small">{p.startTime}–{p.endTime}{p.faculty ? ` · ${p.faculty}` : ''}</span></span>
          <span className={`badge st-${p.status || 'NONE'}`}>{p.status ? STATUS_LABEL[p.status] : '–'}</span>
        </div>
      ))}
    </div>
  );
}

export function WardHome({ api, go, self }) {
  const ward = useApi(() => api.get('/api/ward'), []);
  const day = useApi(() => api.get('/api/day'), []);
  const internals = useApi(() => api.get('/api/internals'), []);
  if (ward.error) return <ErrorNote error={ward.error} onRetry={ward.reload} />;
  if (!ward.data || !day.data) return <Spinner />;
  const w = ward.data;
  const absent = day.data.periods.filter((p) => p.status === 'ABSENT');
  const first = w.name.split(' ')[0];
  const i = internals.data;
  return (
    <>
      <section className="hero">
        <Ring value={w.attendance.percentage == null ? null : Number(w.attendance.percentage)} />
        <div className="hero-text">
          <h2>{w.name}</h2>
          <span className="muted">{w.deptName} · {w.className}</span>
          <span className="small">{w.attendance.present} of {w.attendance.total} periods attended</span>
          {Number(w.attendance.percentage) < w.threshold && <span className="badge lvl-bad" style={{ justifySelf: 'start' }}>Below {w.threshold} %</span>}
        </div>
      </section>
      {absent.length > 0 && (
        <div className="note note-bad" role="alert">
          <span><strong>{self ? 'You were' : `${first} was`} absent in {absent.length} period{absent.length > 1 ? 's' : ''}</strong> on {longDate(day.data.date)}: {absent.map((p) => `P${p.period}`).join(', ')}</span>
          {!self && <button className="btn btn-quiet" onClick={() => go('alerts')}>Alerts</button>}
        </div>
      )}
      {i && (
        <button className="card tap" onClick={() => go('internals')} aria-label="Open internal marks">
          <div className="card-body row" style={{ justifyContent: 'space-between' }}>
            <div><h3>Internal marks</h3><p className="muted small">{i.below > 0 ? `${i.below} subject${i.below > 1 ? 's' : ''} below the pass mark of ${i.passMark}` : 'All subjects above the pass mark'}</p></div>
            <div style={{ textAlign: 'right' }}><div className={`big lvl-text-${level(i.average == null ? null : i.average / 0.4, 50)}`}>{i.average ?? '–'}<span className="muted small"> / 40</span></div><div className="muted small">average</div></div>
          </div>
        </button>
      )}
      <Card title={longDate(day.data.date)} subtitle="All 8 periods of the latest day">
        <PeriodStrip periods={day.data.periods} />
        <div style={{ height: 14 }} />
        <PeriodList periods={day.data.periods} />
      </Card>
      <Card title="Class advisor">
        <p><strong>{w.advisorName || '–'}</strong></p>
        {w.advisorEmail && <p className="muted"><a href={`mailto:${w.advisorEmail}`}>{w.advisorEmail}</a></p>}
      </Card>
    </>
  );
}

export function SubjectAttendance({ rows, title = 'Subject-wise attendance' }) {
  return (
    <Card title={title} subtitle="The line on each bar marks the 75 % limit">
      {rows.length === 0 && <Empty>No attendance yet.</Empty>}
      {rows.map((s) => (
        <div key={s.code} className="subject">
          <div className="subject-top"><div><strong>{s.name}</strong><div className="muted small">{s.code} · {s.faculty}</div></div><PctBadge value={s.percentage} /></div>
          <Progress value={s.percentage} />
          <div className="small muted">
            {s.present} of {s.total} periods attended
            {s.mustAttend != null && (<> · {s.low ? <strong style={{ color: 'var(--bad)' }}>needs {s.mustAttend} more in a row to reach 75 %</strong> : <>can miss {s.canMiss} more and stay above 75 %</>}</>)}
          </div>
        </div>
      ))}
    </Card>
  );
}

export function WardSubjects({ api }) {
  const { data, error, reload } = useApi(() => api.get('/api/subjects'), []);
  if (error) return <ErrorNote error={error} onRetry={reload} />;
  if (!data) return <Spinner />;
  return <SubjectAttendance rows={data} />;
}

export function WardHistory({ api }) {
  const { data, error, reload } = useApi(() => api.get('/api/history?days=30'), []);
  const [open, setOpen] = useState(null);
  const day = useApi(() => (open ? api.get(`/api/day?date=${open}`) : Promise.resolve(null)), [open]);
  if (error) return <ErrorNote error={error} onRetry={reload} />;
  if (!data) return <Spinner />;
  return (
    <div className="hist">
      <h3>Last 30 days</h3>
      {data.length === 0 && <Empty>No attendance yet.</Empty>}
      {data.map((d) => (
        <div key={d.date} className="hist-row">
          <button onClick={() => setOpen(open === d.date ? null : d.date)} aria-expanded={open === d.date}>
            <span><strong>{longDate(d.date)}</strong><br />
              <span className="muted small">{d.absent ? `Absent: ${d.absentPeriods.split(', ').map((p) => `P${p}`).join(', ')}` : 'Present all day'}</span></span>
            <span className={`badge ${d.absent ? 'lvl-bad' : 'lvl-good'}`}>{d.present}/{d.periods}</span>
          </button>
          {open === d.date && <div className="hist-detail">{day.data ? <PeriodList periods={day.data.periods} /> : <Spinner />}</div>}
        </div>
      ))}
    </div>
  );
}

/* ---------- internal assessment ---------- */

const PARTS = [['asg1', 'ASG 1'], ['ct1', 'CT 1'], ['cat1', 'CAT 1'], ['asg2', 'ASG 2'], ['ct2', 'CT 2'], ['cat2', 'CAT 2']];

export function InternalsView({ data, title = 'Internal marks' }) {
  const max = Object.fromEntries((data.maxMarks || []).map((m) => [m.code, m.max]));
  const lv = (v) => level(v == null ? null : (v / 40) * 100, 50);
  return (
    <>
      <div className="kpis">
        <div className={`kpi kpi-${data.average == null ? 'neutral' : lv(data.average) === 'bad' ? 'bad' : lv(data.average) === 'ok' ? 'ok' : 'good'}`}>
          <div className="kpi-label">Average internal mark</div>
          <div className="kpi-value">{data.average ?? '–'}<span className="muted small"> / 40</span></div>
          <div className="kpi-hint">pass mark {data.passMark}</div>
        </div>
        <div className={`kpi kpi-${data.below ? 'bad' : 'good'}`}>
          <div className="kpi-label">Below pass mark</div>
          <div className="kpi-value">{data.below}</div>
          <div className="kpi-hint">of {data.subjects.length} subjects</div>
        </div>
      </div>
      <Card title={title} subtitle="Internal mark = (part 1 + part 2) / 5, where a part = assignment + ⅔ × (class test + CAT)">
        {data.subjects.length === 0 && <Empty>No marks yet.</Empty>}
        {data.subjects.map((s) => (
          <div key={s.code} className="subject">
            <div className="subject-top">
              <div><strong>{s.name}</strong><div className="muted small">{s.code} · {s.faculty}</div></div>
              <span className={`badge lvl-${lv(s.internal)}`}>{s.internal == null ? '–' : `${s.internal} / 40`}</span>
            </div>
            <Progress value={s.internal == null ? null : (s.internal / 40) * 100} threshold={50} />
            <div className="tags">
              {PARTS.map(([k, label]) => (
                <span key={k} className="tag"><span className="muted">{label}</span> <strong>{s[k] ?? '–'}</strong><span className="muted">/{max[k] ?? '?'}</span></span>
              ))}
            </div>
          </div>
        ))}
      </Card>
    </>
  );
}

export function WardInternals({ api }) {
  const { data, error, reload } = useApi(() => api.get('/api/internals'), []);
  if (error) return <ErrorNote error={error} onRetry={reload} />;
  if (!data) return <Spinner />;
  return <InternalsView data={data} />;
}

/** One student seen by an HOD or an advisor: attendance + internals (GET /api/student/{id}). */
export function StudentDetail({ api, id, onClose }) {
  const { data, error, loading, reload } = useApi(() => api.get(`/api/student/${id}`), [id]);
  const rows = (data?.subjects || []).map((s) => ({ ...s }));
  return (
    <div onClick={onClose} style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,.45)', display: 'grid', placeItems: 'center', padding: 16, zIndex: 50 }}>
      <div onClick={(e) => e.stopPropagation()} style={{ width: 'min(820px,100%)', maxHeight: '92vh', overflow: 'auto', display: 'grid', gap: 14 }}>
        <Card title={data ? data.profile.name : 'Student'} subtitle={data ? `#${data.profile.id} · ${data.profile.className} · ${data.profile.deptName}` : ''}
          actions={<button className="btn btn-quiet" onClick={onClose}>Close</button>}>
          {loading && !data && <Spinner />}
          <ErrorNote error={error} onRetry={reload} />
          {data && (
            <div className="row" style={{ gap: 20, alignItems: 'center' }}>
              <Ring value={data.attendance.percentage == null ? null : Number(data.attendance.percentage)} size={112} />
              <div className="small" style={{ display: 'grid', gap: 4 }}>
                <span>{data.attendance.present} of {data.attendance.total} periods attended · {data.attendance.absent} absent</span>
                <span className="muted">Parent: {data.profile.parentName || '–'}{data.profile.parentPhone && <> · <a href={`tel:${data.profile.parentPhone}`}>{data.profile.parentPhone}</a></>}</span>
                <span className="muted">Advisor: {data.profile.advisorName || '–'}</span>
                <span>Internal average: <strong>{data.internals.average ?? '–'} / 40</strong> ({data.internals.below} below pass mark)</span>
              </div>
            </div>
          )}
        </Card>
        {data && <SubjectAttendance rows={rows} title="Attendance by subject" />}
        {data && <InternalsView data={data.internals} title="Internal marks by subject" />}
      </div>
    </div>
  );
}

/* ---------- app frame shared by the Parent and Student portals ---------- */

export function WardShell({ api, portal, session, onOut, subtitle, self }) {
  const alerts = !self;
  const [tab, setTab] = useState('home');
  const [unread, refresh] = useUnread(api, alerts);
  const tabs = [
    { id: 'home', label: 'Today', icon: '🏠' }, { id: 'subjects', label: 'Attendance', icon: '📚' },
    { id: 'internals', label: 'Internals', icon: '📝' }, { id: 'history', label: 'History', icon: '📅' },
    ...(alerts ? [{ id: 'alerts', label: 'Alerts', icon: '🔔' }] : []),
  ];
  const logout = async () => { await api.logout(); api.session.clear(); onOut(); };
  return (
    <div data-portal={portal}>
      <div className="app">
        <div className="topbar">
          <div><strong>{session.fullName}</strong><div className="muted small">{subtitle}</div></div>
          <button className="btn btn-quiet" onClick={logout}>Sign out</button>
        </div>
        {tab === 'home' && <WardHome api={api} go={setTab} self={self} />}
        {tab === 'subjects' && <WardSubjects api={api} />}
        {tab === 'internals' && <WardInternals api={api} />}
        {tab === 'history' && <WardHistory api={api} />}
        {tab === 'alerts' && <Inbox api={api} onChange={refresh} />}
      </div>
      <nav className="tabbar" style={{ gridTemplateColumns: `repeat(${tabs.length}, 1fr)` }} aria-label="Main">
        {tabs.map((t) => (
          <button key={t.id} className={tab === t.id ? 'on' : ''} onClick={() => setTab(t.id)} aria-current={tab === t.id ? 'page' : undefined}>
            <span className="tab-ico" aria-hidden="true">{t.icon}</span>{t.label}
            {t.id === 'alerts' && unread > 0 && <span className="count">{unread}</span>}
          </button>
        ))}
      </nav>
    </div>
  );
}
