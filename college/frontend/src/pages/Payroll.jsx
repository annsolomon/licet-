import React, { useState } from 'react';
import { api } from '../services/api.js';
import { useAction, useApi } from '../hooks/useApi.js';
import { Card, Code, DataTable, ErrorBox, Field, PageHeader, Select, Success } from '../components/ui.jsx';
import { inr, thisMonth } from '../utils/format.js';

export default function Payroll() {
  const [month, setMonth] = useState('2026-09');
  const [empId, setEmpId] = useState('');
  const [slip, setSlip] = useState(null);
  const employees = useApi(() => api.get('/api/employees'), []);
  const payroll = useApi(() => api.get(`/api/payroll?month=${month}`), [month]);
  const gen = useAction();
  const view = useAction();

  const run = async (url, body) => { if (await gen.run(() => api.post(url, body))) payroll.reload(); };
  const showSlip = async (r) => { const res = await view.run(() => api.get(`/api/payroll/${r.id}/payslip`)); if (res) setSlip(res.lines); };

  return (
    <div>
      <PageHeader title="Payroll & payslips" subtitle="Same job done twice: Java OOP classes, or the PL/SQL procedure"
        flow={['React', 'POST /api/payroll/generate', 'PayrollController', 'PayrollService', 'Professor.hra()/da()/pf() (polymorphism)', 'PayrollRepository INSERT', 'payroll table', 'UNIQUE(emp_id, pay_month)']} />
      <Card title="Generate payroll">
        <div className="form-grid">
          <Field label="Month (YYYY-MM)"><input value={month} onChange={(e) => setMonth(e.target.value)} placeholder={thisMonth()} /></Field>
          <Field label="Employee (for one-employee payroll)"><Select value={empId} onChange={setEmpId} options={employees.data?.map((e) => ({ value: e.id, label: `${e.id} ${e.name}` }))} /></Field>
        </div>
        <div className="row wrap">
          <button onClick={() => run('/api/payroll/generate-all', { month })}>All employees (Java)</button>
          <button onClick={() => run('/api/payroll/generate-all-plsql', { month })}>All employees (PL/SQL)</button>
          <button className="secondary" disabled={!empId} onClick={() => run('/api/payroll/generate', { empId: Number(empId), month })}>One employee (Java)</button>
          <button className="secondary" disabled={!empId} onClick={() => run('/api/payroll/generate-plsql', { empId: Number(empId), month })}>One employee (PL/SQL)</button>
        </div>
        <ErrorBox error={gen.error} />
        {gen.result && <Success>Done: {JSON.stringify(gen.result.created !== undefined ? gen.result : { payrollId: gen.result.id, net: gen.result.net })}</Success>}
      </Card>
      <ErrorBox error={payroll.error || view.error} />
      <DataTable
        columns={[{ key: 'empName', label: 'Employee' }, { key: 'designation', label: 'Designation' },
          ...['basic', 'hra', 'da', 'pf', 'gross', 'net'].map((k) => ({ key: k, label: k.toUpperCase(), render: (r) => inr(r[k]) })),
          { key: 'p', label: '', render: (r) => <button className="small" onClick={() => showSlip(r)}>Payslip</button> }]}
        rows={payroll.data} empty="No payroll for this month yet" />
      {slip && <Card title="Payslip" actions={<button className="small secondary" onClick={() => setSlip(null)}>Close</button>}><Code>{slip}</Code></Card>}
    </div>
  );
}
