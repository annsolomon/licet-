import React, { useState } from 'react';
import { createApi } from '@shared/api.js';
import { Login } from '@shared/ui.jsx';
import { WardShell } from '@shared/ward.jsx';

const api = createApi('student');

export default function App() {
  const [session, setSession] = useState(api.session.get());
  if (!session) {
    return (
      <Login portal="student" api={api} onLogin={setSession} icon="🎓" title="Student Portal"
        subtitle="Your attendance, period by period, and your internal marks."
        accounts={[
          { username: 's101', password: 'student123', note: 'Student 101' },
          { username: 's104', password: 'student123', note: 'Student 104: low attendance and internals' },
        ]} />
    );
  }
  return <WardShell api={api} portal="student" session={session} onOut={() => setSession(null)} subtitle="Student Portal" self />;
}
