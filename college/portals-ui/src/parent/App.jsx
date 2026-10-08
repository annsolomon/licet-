import React, { useState } from 'react';
import { createApi } from '@shared/api.js';
import { Login } from '@shared/ui.jsx';
import { WardShell } from '@shared/ward.jsx';

const api = createApi('parent');

export default function App() {
  const [session, setSession] = useState(api.session.get());
  if (!session) {
    return (
      <Login portal="parent" api={api} onLogin={setSession} icon="👨‍👩‍👧" title="Parent Portal"
        subtitle="Attendance, alerts and internal marks of your ward."
        accounts={[
          { username: 'p101', password: 'parent123', note: 'Parent of student 101' },
          { username: 'p104', password: 'parent123', note: 'Student 104: low attendance and internals' },
        ]} />
    );
  }
  return <WardShell api={api} portal="parent" session={session} onOut={() => setSession(null)} subtitle="Parent Portal" />;
}
