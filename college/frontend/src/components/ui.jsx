import React from 'react';

/** Shows an API error together with the LAYER that detected it (great for the viva). */
export function ErrorBox({ error }) {
  if (!error) return null;
  return (
    <div className="alert error">
      <strong>{error.layer ? `[${error.layer}] ` : ''}</strong>
      {error.message}
      {error.status ? <span className="muted"> (HTTP {error.status})</span> : null}
    </div>
  );
}

export const Success = ({ children }) => (children ? <div className="alert ok">{children}</div> : null);
export const Loading = ({ show = true }) => (show ? <p className="muted">Loading...</p> : null);

export function Card({ title, children, actions }) {
  return (
    <section className="card">
      {(title || actions) && (
        <header>
          <h3>{title}</h3>
          <div className="row">{actions}</div>
        </header>
      )}
      {children}
    </section>
  );
}

/** columns = [{ key, label, render?(row) }] */
export function DataTable({ columns, rows, empty = 'No rows', onRowClick, rowClass }) {
  if (!rows) return null;
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>{columns.map((c) => <th key={c.key}>{c.label}</th>)}</tr>
        </thead>
        <tbody>
          {rows.length === 0 && <tr><td colSpan={columns.length} className="muted">{empty}</td></tr>}
          {rows.map((r, i) => (
            <tr key={r.id ?? r.recordId ?? i} onClick={onRowClick ? () => onRowClick(r) : undefined}
                className={`${onRowClick ? 'clickable' : ''} ${rowClass ? rowClass(r) : ''}`}>
              {columns.map((c) => <td key={c.key}>{c.render ? c.render(r) : r[c.key] ?? '-'}</td>)}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

/** The "how does this work" strip:  React -> Controller -> Service -> Repository -> SQL -> Oracle */
export function FlowDiagram({ steps, title = 'How this page works' }) {
  return (
    <details className="flow">
      <summary>{title}</summary>
      <div className="flow-steps">
        {steps.map((s, i) => (
          <React.Fragment key={s}>
            <span className="flow-step">{s}</span>
            {i < steps.length - 1 && <span className="flow-arrow">&rarr;</span>}
          </React.Fragment>
        ))}
      </div>
    </details>
  );
}

export function PageHeader({ title, subtitle, flow, children }) {
  return (
    <div className="page-head">
      <div>
        <h2>{title}</h2>
        {subtitle && <p className="muted">{subtitle}</p>}
      </div>
      <div className="row">{children}</div>
      {flow && <FlowDiagram steps={flow} />}
    </div>
  );
}

export function Field({ label, children }) {
  return <label className="field"><span>{label}</span>{children}</label>;
}

export function Select({ value, onChange, options, placeholder = 'Select...' }) {
  return (
    <select value={value ?? ''} onChange={(e) => onChange(e.target.value)}>
      <option value="">{placeholder}</option>
      {(options || []).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
    </select>
  );
}

export const Code = ({ children }) => <pre className="code">{Array.isArray(children) ? children.join('\n') : children}</pre>;
