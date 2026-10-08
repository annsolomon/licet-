import React, { useState } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { session } from './services/api.js';
import Layout from './components/Layout.jsx';
import CrudPage from './components/CrudPage.jsx';
import Login from './pages/Login.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Students from './pages/Students.jsx';
import Attendance from './pages/Attendance.jsx';
import Marks from './pages/Marks.jsx';
import Results from './pages/Results.jsx';
import Grades from './pages/Grades.jsx';
import Payroll from './pages/Payroll.jsx';
import Reports from './pages/Reports.jsx';
import Audit from './pages/Audit.jsx';
import JavaLab from './pages/JavaLab.jsx';
import DbLab from './pages/DbLab.jsx';
import { classConfig, departmentConfig, employeeConfig, facultyConfig, subjectConfig } from './pages/crudConfigs.jsx';

export default function App() {
  const [user, setUser] = useState(() => (session.token ? session.user() : null));
  if (!user) {
    return (
      <Routes>
        <Route path="/login" element={<Login onLogin={setUser} />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    );
  }
  const role = user.role;
  const admin = (el) => (role === 'ADMIN' ? el : <Navigate to="/" replace />);
  return (
    <Routes>
      <Route path="/login" element={<Navigate to="/" replace />} />
      <Route element={<Layout user={user} onLogout={() => setUser(null)} />}>
        <Route index element={<Dashboard />} />
        <Route path="departments" element={<CrudPage config={departmentConfig} role={role} />} />
        <Route path="students" element={<Students role={role} />} />
        <Route path="faculty" element={<CrudPage config={facultyConfig} role={role} />} />
        <Route path="subjects" element={<CrudPage config={subjectConfig} role={role} />} />
        <Route path="classes" element={<CrudPage config={classConfig} role={role} />} />
        <Route path="attendance" element={<Attendance role={role} />} />
        <Route path="marks" element={<Marks />} />
        <Route path="results" element={<Results role={role} />} />
        <Route path="grades" element={<Grades role={role} />} />
        <Route path="employees" element={admin(<CrudPage config={employeeConfig} role={role} />)} />
        <Route path="payroll" element={admin(<Payroll />)} />
        <Route path="reports" element={<Reports role={role} />} />
        <Route path="audit" element={admin(<Audit />)} />
        <Route path="java-lab" element={<JavaLab />} />
        <Route path="db-lab" element={<DbLab />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
