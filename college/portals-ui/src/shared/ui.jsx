import React, { useEffect, useState } from 'react';
import { useAction, useApi } from './hooks.js';
import { level, pct } from './format.js';

export const Spinner = () => <div className="spinner" role="status" aria-label="Loading" />;

export function ErrorNote({ error, onRetry }) {
  if (!error) return null;
  return (
    <div className="note note-bad" role="alert">
      <span>{error.message}</span>
      {onRetry && <button className="btn btn-quiet" onClick={onRetry}>Retry</button>}
    </div>
  );
}

export const Empty = ({ children }) => <div className="empty">{children}</div>;

export function Card({ title, subtitle, actions, children, pad = true, className = '' }) {
  return (
    <section className={`card ${className}`}>
      {(title || actions) && (
        <header className="card-head">
          <div>
            {title && <h3>{title}</h3>}
            {subtitle && <p className="muted small">{subtitle}</p>}
          </div>
          {actions && <div className="row">{actions}</div>}
        </header>
      )}
      <div className={pad ? 'card-body' : ''}>{children}</div>
    </section>
  );
}

export function Kpi({ label, value, hint, tone = 'neutral' }) {
  return (
    <div className={`kpi kpi-${tone}`}>
      <div className="kpi-label">{label}</div>
      <div className="kpi-value">{value}</div>
      {hint && <div className="kpi-hint">{hint}</div>}
    </div>
  );
}

export function Progress({ value, threshold = 75 }) {
  const v = value == null ? 0 : Math.min(100, Number(value));
  return (
    <div className="progress" role="progressbar" aria-valuenow={v} aria-valuemin="0" aria-valuemax="100">
      <div className={`progress-fill lvl-${level(value, threshold)}`} style={{ width: `${v}%` }} />
      <div className="progress-mark" style={{ left: `${threshold}%` }} title={`${threshold}% limit`} />
    </div>
  );
}

export const PctBadge = ({ value, threshold = 75 }) => <span className={`badge lvl-${level(value, threshold)}`}>{pct(value)}</span>;

export function Segmented({ options, value, onChange }) {
  return (
    <div className="segmented" role="tablist">
      {options.map((o) => (
        <button key={o.value} role="tab" aria-selected={value === o.value} className={value === o.value ? 'on' : ''} onClick={() => onChange(o.value)}>
          {o.label}
        </button>
      ))}
    </div>
  );
}

export function DateField({ value, onChange, label = 'Date' }) {
  return (
    <label className="field inline">
      <span>{label}</span>
      <input type="date" value={value || ''} onChange={(e) => onChange(e.target.value)} />
    </label>
  );
}

export function SearchField({ value, onChange, placeholder = 'Search' }) {
  return <input className="search" type="search" placeholder={placeholder} value={value} onChange={(e) => onChange(e.target.value)} aria-label={placeholder} />;
}

/** Plain table. columns = [{ key, label, render?, align?, className? }] */
export function Table({ columns, rows, empty = 'Nothing to show', rowKey = 'id', onRow }) {
  if (!rows) return null;
  if (rows.length === 0) return <Empty>{empty}</Empty>;
  return (
    <div className="table-wrap">
      <table>
        <thead><tr>{columns.map((c) => <th key={c.key} style={{ textAlign: c.align }}>{c.label}</th>)}</tr></thead>
        <tbody>
          {rows.map((r, i) => (
            <tr key={r[rowKey] ?? i} onClick={onRow ? () => onRow(r) : undefined} className={onRow ? 'click' : ''}>
              {columns.map((c) => <td key={c.key} style={{ textAlign: c.align }} className={c.className}>{c.render ? c.render(r) : r[c.key] ?? '–'}</td>)}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function Login({ portal, title, subtitle, accounts, onLogin, api, icon }) {
  const [username, setUsername] = useState(accounts[0].username);
  const [password, setPassword] = useState(accounts[0].password);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const s = await api.login(username, password);
      api.session.set(s);
      onLogin(s);
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="login" data-portal={portal}>
      <form className="login-card" onSubmit={submit}>
        <div className="login-icon" aria-hidden="true">{icon}</div>
        <h1>{title}</h1>
        <p className="muted">{subtitle}</p>
        <label className="field"><span>Username</span><input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" autoFocus /></label>
        <label className="field"><span>Password</span><input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" /></label>
        <button className="btn btn-primary" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
        <ErrorNote error={error} />
        <details className="demo">
          <summary>Demo accounts</summary>
          {accounts.map((a) => (
            <button type="button" key={a.username} className="demo-row" onClick={() => { setUsername(a.username); setPassword(a.password); }}>
              <strong>{a.username}</strong><span className="muted"> / {a.password}</span><em>{a.note}</em>
            </button>
          ))}
        </details>
      </form>
    </main>
  );
}

/** Shell with sidebar (desktop) / top bar (mobile) for the HOD and advisor portals. */
export function SidebarShell({ portal, brand, user, nav, page, onNav, onLogout, badge = {}, children }) {
  return (
    <div className="shell" data-portal={portal}>
      <aside className="side">
        <div className="brand"><span className="brand-mark" aria-hidden="true" />{brand}</div>
        <nav aria-label="Main">
          {nav.map((n) => (
            <button key={n.id} className={page === n.id ? 'on' : ''} onClick={() => onNav(n.id)}>
              <span>{n.label}</span>
              {badge[n.id] > 0 && <span className="count">{badge[n.id]}</span>}
            </button>
          ))}
        </nav>
        <div className="who">
          <div className="who-name">{user.fullName}</div>
          <button className="btn btn-quiet" onClick={onLogout}>Sign out</button>
        </div>
      </aside>
      <main className="main">{children}</main>
    </div>
  );
}

export function PageHead({ title, subtitle, children }) {
  return (
    <div className="page-head">
      <div><h2>{title}</h2>{subtitle && <p className="muted">{subtitle}</p>}</div>
      <div className="row">{children}</div>
    </div>
  );
}

/** Alerts list shared by the parent and advisor portals. */
export function Inbox({ api, onChange }) {
  const { data, error, loading, reload } = useApi(() => api.get('/api/notifications'), []);
  const act = useAction();
  const open = async (n) => {
    if (n.read) return;
    await act.run(() => api.post(`/api/notifications/${n.id}/read`));
    reload();
    onChange?.();
  };
  const readAll = async () => {
    await act.run(() => api.post('/api/notifications/read-all'));
    reload();
    onChange?.();
  };
  const unread = (data || []).filter((n) => !n.read).length;
  return (
    <Card title="Alerts" subtitle={unread ? `${unread} unread` : 'All caught up'}
      actions={unread > 0 && <button className="btn btn-quiet" onClick={readAll}>Mark all read</button>}>
      {loading && !data && <Spinner />}
      <ErrorNote error={error || act.error} onRetry={reload} />
      {data && data.length === 0 && <Empty>No alerts yet. An alert appears here as soon as a period is marked absent.</Empty>}
      <ul className="inbox">
        {(data || []).map((n) => (
          <li key={n.id} className={n.read ? '' : 'unread'}>
            <button onClick={() => open(n)}>
              <span className="dot-unread" aria-hidden="true" />
              <span className="inbox-main">
                <strong>{n.title}</strong>
                <span>{n.message}</span>
                <span className="muted small">{n.channel === 'SMS' ? 'SMS to' : 'Email to'} {n.destination || '–'} · {n.createdAt} · {n.status === 'SENT' ? 'delivered' : n.status.toLowerCase()}</span>
              </span>
            </button>
          </li>
        ))}
      </ul>
    </Card>
  );
}

/** Unread count, refreshed every 20 s. */
export function useUnread(api, enabled = true) {
  const [n, setN] = useState(0);
  const load = () => (enabled ? api.get('/api/notifications/unread-count').then((r) => setN(r.unread), () => {}) : undefined);
  useEffect(() => { if (!enabled) return undefined; load(); const t = setInterval(load, 20000); return () => clearInterval(t); }, [enabled]);
  return [n, load];
}
