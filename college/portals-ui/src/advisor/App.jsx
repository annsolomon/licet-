import React, { useState } from 'react';
import { createApi } from '@shared/api.js';
import { Inbox, Login, PageHead, SidebarShell, useUnread } from '@shared/ui.jsx';
import Today from './Today.jsx';
import { Subjects, AtRisk } from './Pages.jsx';
import { InternalsSummary, StudentsPage, internalBadge } from '@shared/people.jsx';
import InternalsGrid from './Internals.jsx';

const api = createApi('advisor');
const PARENT_COLS = [
  { key: 'absentToday', label: 'Absent (latest day)', align: 'right', render: (r) => (r.absentToday ? <span className="badge lvl-bad">{r.absentToday} periods</span> : '–') },
  { key: 'parentName', label: 'Parent', render: (r) => (r.parentPhone ? <a href={`tel:${r.parentPhone}`} onClick={(e) => e.stopPropagation()}>{r.parentName} · {r.parentPhone}</a> : '–') },
];

export default function App() {
  const [session, setSession] = useState(api.session.get());
  const [page, setPage] = useState('today');
  if (!session) {
    return (
      <Login portal="advisor" api={api} onLogin={setSession} icon="🧑‍🏫" title="Class Advisor Portal"
        subtitle="Every period of your class, with instant absence alerts."
        accounts={[
          { username: 'advisor.cse', password: 'advisor123', note: 'CSE class advisor' },
          { username: 'advisor.ece', password: 'advisor123', note: 'ECE class advisor' },
          { username: 'advisor.eee', password: 'advisor123', note: 'EEE class advisor' },
          { username: 'advisor.mech', password: 'advisor123', note: 'MECH class advisor' },
        ]} />
    );
  }
  return <Authed session={session} page={page} setPage={setPage} onOut={() => setSession(null)} />;
}

function Authed({ session, page, setPage, onOut }) {
  const [unread, refresh] = useUnread(api);
  const nav = [
    { id: 'today', label: 'Periods' }, { id: 'students', label: 'Students' }, { id: 'internals', label: 'Internals' }, { id: 'subjects', label: 'Subjects' },
    { id: 'risk', label: 'At-risk' }, { id: 'alerts', label: 'Alerts' },
  ];
  const logout = async () => { await api.logout(); api.session.clear(); onOut(); };
  return (
    <SidebarShell portal="advisor" brand="Class Advisor" user={session} nav={nav} page={page} onNav={setPage} onLogout={logout} badge={{ alerts: unread }}>
      {page === 'today' && <Today api={api} onMarked={refresh} />}
      {page === 'students' && <StudentsPage api={api} withClass={false} extra={PARENT_COLS} />}
      {page === 'internals' && <InternalsGrid api={api} />}
      {page === 'subjects' && <Subjects api={api} />}
      {page === 'risk' && <AtRisk api={api} />}
      {page === 'alerts' && <><PageHead title="Alerts" subtitle="Sent when one of your students is marked absent" /><Inbox api={api} onChange={refresh} /></>}
    </SidebarShell>
  );
}
