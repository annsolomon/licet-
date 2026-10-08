import React, { useState } from 'react';
import { api } from '../services/api.js';
import { useApi, useAction } from '../hooks/useApi.js';
import { Card, DataTable, ErrorBox, Field, Loading, PageHeader, Select, Success } from './ui.jsx';

/**
 * One generic list + form page used by Departments, Students, Faculty, Subjects, Classes and Employees.
 *
 * config = { title, subtitle, endpoint, flow, columns, fields, canWrite }
 * field  = { name, label, type: text|number|email|select, options?: () => Promise<[{value,label}]>, readOnlyOnEdit }
 */
export default function CrudPage({ config, role }) {
  const { title, subtitle, endpoint, flow, columns, fields, toForm, extraFilter } = config;
  const canWrite = role === 'ADMIN';
  const [query, setQuery] = useState('');
  const list = useApi(() => api.get(extraFilter ? extraFilter(query) : endpoint), [query, config.filterKey]);
  const optionLists = useApi(async () => {
    const out = {};
    for (const f of fields) if (f.options) out[f.name] = await f.options();
    return out;
  }, []);
  const [form, setForm] = useState(null);       // null = form closed
  const [editingId, setEditingId] = useState(null);
  const save = useAction();
  const remove = useAction();

  const open = (row) => {
    setEditingId(row ? row.id : null);
    setForm(row ? toForm(row) : Object.fromEntries(fields.map((f) => [f.name, ''])));
    save.clear();
  };

  const submit = async (e) => {
    e.preventDefault();
    const body = { ...form };
    fields.forEach((f) => {
      if (f.type === 'number' || f.type === 'select') body[f.name] = body[f.name] === '' ? null : Number(body[f.name]);
    });
    const ok = await save.run(() => (editingId ? api.put(`${endpoint}/${editingId}`, body) : api.post(`${endpoint}${config.saveQuery || ''}`, body)));
    if (ok) { setForm(null); list.reload(); }
  };

  const del = async (row) => {
    if (!window.confirm(`Delete ${row.name || row.code || row.id}?`)) return;
    await remove.run(() => api.del(`${endpoint}/${row.id}`));
    list.reload();
  };

  const cols = [...columns];
  if (canWrite) {
    cols.push({
      key: '_actions', label: '',
      render: (r) => (
        <span className="row">
          <button className="small" onClick={(e) => { e.stopPropagation(); open(r); }}>Edit</button>
          <button className="small danger" onClick={(e) => { e.stopPropagation(); del(r); }}>Delete</button>
        </span>
      ),
    });
  }

  return (
    <div>
      <PageHeader title={title} subtitle={subtitle} flow={flow}>
        {extraFilter && <input placeholder="Search..." value={query} onChange={(e) => setQuery(e.target.value)} />}
        {config.searchModes}
        {canWrite && <button onClick={() => open(null)}>+ Add</button>}
      </PageHeader>
      <ErrorBox error={list.error || remove.error} />
      <Loading show={list.loading && !list.data} />
      {form && (
        <Card title={editingId ? `Edit ${title}` : `Add ${title}`}>
          <form onSubmit={submit} className="form-grid">
            {fields.map((f) => (
              <Field key={f.name} label={f.label}>
                {f.type === 'select' ? (
                  <Select value={form[f.name]} onChange={(v) => setForm({ ...form, [f.name]: v })}
                          options={optionLists.data?.[f.name]} />
                ) : (
                  <input type={f.type === 'number' ? 'number' : f.type || 'text'} value={form[f.name] ?? ''}
                         disabled={editingId && f.readOnlyOnEdit}
                         onChange={(e) => setForm({ ...form, [f.name]: e.target.value })} />
                )}
              </Field>
            ))}
            <div className="row full">
              <button type="submit" disabled={save.busy}>{save.busy ? 'Saving...' : 'Save'}</button>
              <button type="button" className="secondary" onClick={() => setForm(null)}>Cancel</button>
            </div>
          </form>
          <ErrorBox error={save.error} />
        </Card>
      )}
      {config.afterForm}
      <DataTable columns={cols} rows={list.data} />
      {config.notice && <Success>{config.notice}</Success>}
    </div>
  );
}
