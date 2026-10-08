import React, { useState } from 'react';
import { createApi } from '@shared/api.js';
import { Login, SidebarShell } from '@shared/ui.jsx';
import Overview from './Overview.jsx';
import { Classes, Subjects, Faculty, AtRisk, Absentees } from './Pages.jsx';
import { InternalsSummary, StudentsPage } from '@shared/people.jsx';
import { PageHead } from '@shared/ui.jsx';

const api = createApi('hod');
const NAV = [
  { id: 'overview', label: 'Overview' },
  { id: 'absentees', label: 'Absentees' },
  { id: 'students', label: 'Students' },
  { id: 'internals', label: 'Internals' },
  { id: 'classes', label: 'Classes' },
  { id: 'subjects', label: 'Subjects' },
  { id: 'faculty', label: 'Faculty' },
  { id: 'risk', label: 'At-risk students' },
];

export default function App() {
  const [session, setSession] = useState(api.session.get());
  const [page, setPage] = useState('overview');

  if (!session) {
    return (
      <Login portal="hod" api={api} onLogin={setSession} icon="🏛️" title="HOD Portal"
        subtitle="Department-wide attendance, period by period."
        accounts={[
          { username: 'hod.cse', password: 'hod123', note: 'Head of CSE' },
          { username: 'hod.ece', password: 'hod123', note: 'Head of ECE' },
          { username: 'hod.eee', password: 'hod123', note: 'Head of EEE' },
          { username: 'hod.mech', password: 'hod123', note: 'Head of MECH' },
        ]} />
    );
  }
  const logout = async () => { await api.logout(); api.session.clear(); setSession(null); };
  const Internals = ({ api }) => (<><PageHead title="Internal marks" subtitle="Assignments, class tests and CATs of the whole department" /><InternalsSummary api={api} /></>);
  const Page = { overview: Overview, students: StudentsPage, internals: Internals, absentees: Absentees, classes: Classes, subjects: Subjects, faculty: Faculty, risk: AtRisk }[page];
  return (
    <SidebarShell portal="hod" brand="HOD Portal" user={session} nav={NAV} page={page} onNav={setPage} onLogout={logout}>
      <Page api={api} go={setPage} subtitle="Everybody in the department. Click a student for attendance by subject and internal marks." />
    </SidebarShell>
  );
}
