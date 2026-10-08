import React, { useState } from 'react';
import CrudPage from '../components/CrudPage.jsx';
import { qs } from '../services/api.js';
import { studentBase } from './crudConfigs.jsx';

/** Search runs either in Java (GenericSearch) or in Oracle (WHERE ... LIKE). "Let Oracle check" skips Java duplicate checks. */
export default function Students({ role }) {
  const [mode, setMode] = useState('java');
  const [oracle, setOracle] = useState(false);
  const config = {
    ...studentBase,
    filterKey: mode,
    saveQuery: oracle ? '?skipChecks=true' : '',
    extraFilter: (q) => `/api/students${qs({ search: q, mode })}`,
    searchModes: (
      <>
        <select value={mode} onChange={(e) => setMode(e.target.value)}>
          <option value="java">Search in Java (GenericSearch)</option>
          <option value="sql">Search in Oracle (LIKE)</option>
        </select>
        {role === 'ADMIN' && (
          <label className="check" title="Java skips duplicate checks, so the Oracle constraint rejects the row (ORA-00001)">
            <input type="checkbox" checked={oracle} onChange={(e) => setOracle(e.target.checked)} /> let Oracle reject duplicates
          </label>
        )}
      </>
    ),
  };
  return <CrudPage config={config} role={role} />;
}
