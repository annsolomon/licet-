// One tiny API client per portal. The token is kept per portal, so the three portals never share a login.
export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

export function createApi(portal) {
  const KEY = `college.${portal}.session`;
  const store = {
    get() { try { return JSON.parse(localStorage.getItem(KEY)); } catch { return null; } },
    set(v) { try { localStorage.setItem(KEY, JSON.stringify(v)); } catch { /* storage blocked */ } },
    clear() { try { localStorage.removeItem(KEY); } catch { /* ignore */ } },
  };

  async function request(method, url, body) {
    const headers = {};
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    const s = store.get();
    if (s?.token) headers.Authorization = `Bearer ${s.token}`;
    let res;
    try {
      res = await fetch(url, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
    } catch {
      throw new ApiError(0, 'Cannot reach the service. Is it running? (./portals-run.sh status)');
    }
    const text = await res.text();
    let data = null;
    try { data = text ? JSON.parse(text) : null; } catch { data = { message: text }; }
    if (res.status === 401 && !url.endsWith('/login')) {
      store.clear();
      window.location.reload();
    }
    if (!res.ok) throw new ApiError(res.status, data?.message || `Request failed (${res.status})`);
    return data;
  }

  return {
    overviewPath: portal === 'hod' ? '/students' : '/overview',
    session: store,
    get: (u) => request('GET', u),
    post: (u, b) => request('POST', u, b ?? {}),
    put: (u, b) => request('PUT', u, b),
    login: (username, password) => request('POST', '/api/login', { username, password }),
    logout: () => request('POST', '/api/logout').catch(() => null),
  };
}
