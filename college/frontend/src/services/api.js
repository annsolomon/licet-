// All HTTP calls live here. Every call:  React page -> api.js -> fetch('/api/...') -> Spring Boot controller.
const TOKEN_KEY = 'college.token';

export const session = {
  get token() { try { return localStorage.getItem(TOKEN_KEY); } catch { return null; } },
  save(login) { try { localStorage.setItem(TOKEN_KEY, login.token); localStorage.setItem('college.user', JSON.stringify(login)); } catch { /* private mode */ } },
  user() { try { return JSON.parse(localStorage.getItem('college.user')); } catch { return null; } },
  clear() { try { localStorage.removeItem(TOKEN_KEY); localStorage.removeItem('college.user'); } catch { /* ignore */ } },
};

export class ApiError extends Error {
  constructor(status, body) {
    super(body?.message || `Request failed (${status})`);
    this.status = status;
    this.layer = body?.layer;     // which layer found the problem: Java validation | Oracle constraint | Trigger ...
    this.code = body?.error;
  }
}

async function request(method, url, body) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (session.token) headers.Authorization = `Bearer ${session.token}`;
  let res;
  try {
    res = await fetch(url, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  } catch {
    throw new ApiError(0, { message: 'Cannot reach the backend on port 8080. Is it running? (./run.sh)', layer: 'Network' });
  }
  if (res.status === 401 && !url.endsWith('/auth/login')) {
    session.clear();
    window.location.assign('/login');
  }
  if (res.status === 204) return null;
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = { message: text }; }
  if (!res.ok) throw new ApiError(res.status, data);
  return data;
}

export const api = {
  get: (url) => request('GET', url),
  post: (url, body) => request('POST', url, body ?? {}),
  put: (url, body) => request('PUT', url, body),
  del: (url) => request('DELETE', url),
  async download(url, filename) {
    const res = await fetch(url, { headers: { Authorization: `Bearer ${session.token}` } });
    if (!res.ok) throw new ApiError(res.status, await res.json().catch(() => null));
    const blob = await res.blob();
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = filename;
    a.click();
    URL.revokeObjectURL(a.href);
  },
};

export const qs = (params) => {
  const p = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') p.set(k, v); });
  const s = p.toString();
  return s ? `?${s}` : '';
};
