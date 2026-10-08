import React from 'react';
import { api } from '../services/api.js';
import { useApi } from '../hooks/useApi.js';
import { Card, ErrorBox, Loading, PageHeader } from '../components/ui.jsx';
import { num } from '../utils/format.js';

function Bars({ data }) {
  const entries = Object.entries(data || {});
  const max = Math.max(1, ...entries.map(([, v]) => v));
  return (
    <div className="bars">
      {entries.map(([k, v]) => (
        <div key={k} className="bar-row">
          <span className="bar-label">{k}</span>
          <div className="bar"><div style={{ width: `${(v / max) * 100}%` }} /></div>
          <span className="bar-value">{v}</span>
        </div>
      ))}
    </div>
  );
}

export default function Dashboard() {
  const { data, error, loading } = useApi(() => api.get('/api/dashboard'), []);
  const stat = (label, value, cls = '') => (
    <div className={`stat ${cls}`}><div className="stat-value">{value}</div><div className="stat-label">{label}</div></div>
  );
  return (
    <div>
      <PageHeader title="Dashboard" subtitle="Live numbers from Oracle" flow={data?.flow || ['React Dashboard', 'GET /api/dashboard', 'DashboardController', 'DashboardService', 'Repository', 'JdbcTemplate', 'Oracle']} />
      <ErrorBox error={error} />
      <Loading show={loading} />
      {data && (
        <>
          <div className="stats">
            {stat('Students', data.students)}{stat('Faculty', data.faculty)}{stat('Subjects', data.subjects)}
            {stat('Departments', data.departments)}{stat('Classes', data.classes)}{stat('Employees', data.employees)}
            {stat('Average attendance %', num(data.averageAttendance))}
            {stat('Students below 75 %', data.lowAttendanceStudents, data.lowAttendanceStudents > 0 ? 'warn' : '')}
            {stat('Pass %', num(data.passPercentage, 1))}
            {stat('Failed results', data.failCount, data.failCount > 0 ? 'warn' : '')}
          </div>
          <div className="two">
            <Card title="Students per department"><Bars data={data.studentsByDepartment} /></Card>
            <Card title="Grade distribution"><Bars data={data.gradeDistribution} /></Card>
          </div>
        </>
      )}
    </div>
  );
}
