import React from 'react';
import { api } from '../services/api.js';
import { useApi } from '../hooks/useApi.js';
import { DataTable, ErrorBox, PageHeader } from '../components/ui.jsx';

export default function Audit() {
  const audit = useApi(() => api.get('/api/audit?limit=200'), []);
  return (
    <div>
      <PageHeader title="Audit log" subtitle="Rows written by Oracle TRIGGERS (Java never writes here)"
        flow={['Any INSERT/UPDATE/DELETE on student or marks', 'Oracle trigger fires', 'INSERT INTO audit_log', 'AuditRepository SELECT', 'GET /api/audit', 'this table']}>
        <button className="secondary" onClick={audit.reload}>Refresh</button>
      </PageHeader>
      <ErrorBox error={audit.error} />
      <DataTable columns={[{ key: 'changedDate', label: 'When' }, { key: 'tableName', label: 'Table' }, { key: 'operation', label: 'Operation' },
        { key: 'recordKey', label: 'Key' }, { key: 'oldValue', label: 'Old' }, { key: 'newValue', label: 'New' }, { key: 'changedBy', label: 'By' }]}
        rows={audit.data} empty="No audit rows yet. Change a student or a mark and refresh." />
    </div>
  );
}
