import React from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { api, session } from '../services/api.js';

const NAV = [
  ['/', 'Dashboard'], ['/departments', 'Departments'], ['/students', 'Students'], ['/faculty', 'Faculty'],
  ['/subjects', 'Subjects'], ['/classes', 'Classes'], ['/attendance', 'Attendance'], ['/marks', 'Internal / Marks'],
  ['/results', 'Results & GPA'], ['/grades', 'Grades'], ['/employees', 'Employees', 'ADMIN'],
  ['/payroll', 'Payroll & Payslips', 'ADMIN'], ['/reports', 'Reports'], ['/audit', 'Audit Log', 'ADMIN'],
  ['/java-lab', 'Java Lab'], ['/db-lab', 'DBMS Lab'],
];

export default function Layout({ user, onLogout }) {
  const navigate = useNavigate();
  const logout = async () => {
    try { await api.post('/api/auth/logout'); } catch { /* token may already be gone */ }
    session.clear();
    onLogout();
    navigate('/login');
  };
  return (
    <div className="shell">
      <aside>
        <h1>College AMS</h1>
        <nav>
          {NAV.filter(([, , role]) => !role || role === user.role).map(([to, label]) => (
            <NavLink key={to} to={to} end={to === '/'}>{label}</NavLink>
          ))}
        </nav>
        <div className="who">
          <div>{user.fullName}</div>
          <small>{user.role}</small>
          <button className="small secondary" onClick={logout}>Log out</button>
        </div>
      </aside>
      <main><Outlet /></main>
    </div>
  );
}
