import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, session } from '../services/api.js';
import { useAction } from '../hooks/useApi.js';
import { ErrorBox, FlowDiagram } from '../components/ui.jsx';

export default function Login({ onLogin }) {
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin123');
  const action = useAction();
  const navigate = useNavigate();

  const submit = async (e) => {
    e.preventDefault();
    const res = await action.run(() => api.post('/api/auth/login', { username, password }));
    if (res) { session.save(res); onLogin(res); navigate('/'); }
  };

  return (
    <div className="login">
      <form className="card" onSubmit={submit}>
        <h2>College Academic Management System</h2>
        <p className="muted">React + Spring Boot + JdbcTemplate + Oracle</p>
        <label className="field"><span>Username</span><input value={username} onChange={(e) => setUsername(e.target.value)} autoFocus /></label>
        <label className="field"><span>Password</span><input type="password" value={password} onChange={(e) => setPassword(e.target.value)} /></label>
        <button type="submit" disabled={action.busy}>{action.busy ? 'Signing in...' : 'Log in'}</button>
        <ErrorBox error={action.error} />
        <p className="muted small-print">
          Demo accounts: <code>admin / admin123</code> (full access) and <code>faculty / faculty123</code> (attendance, marks, results, reports).
        </p>
        <FlowDiagram title="How login works" steps={['Login form', 'POST /api/auth/login', 'AuthController', 'AuthService', 'UserRepository', 'SELECT ... FROM app_user', 'Oracle', 'SHA-256 compare', 'token']} />
      </form>
    </div>
  );
}
