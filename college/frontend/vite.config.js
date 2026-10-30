import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Local: the browser talks only to :5173; Vite forwards /api to Spring Boot on :8080 (no CORS trouble in Codespaces).
// Deployed (GitHub Pages): VITE_BASE=/repo-name/, VITE_API_URL=https://backend..., VITE_HASH_ROUTER=true
const apiUrl = process.env.VITE_API_URL || '';

// Content-Security-Policy for the static build: scripts only from this site, API calls only to the backend.
const csp = () => ({
  name: 'csp',
  apply: 'build',
  transformIndexHtml(html) {
    const connect = ["'self'", apiUrl ? new URL(apiUrl).origin : ''].filter(Boolean).join(' ');
    const policy = `default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; `
      + `connect-src ${connect}; object-src 'none'; base-uri 'self'; form-action 'self'`;
    return html.replace('<head>', `<head>\n    <meta http-equiv="Content-Security-Policy" content="${policy}" />`
      + '\n    <meta name="referrer" content="no-referrer" />');
  },
});

export default defineConfig({
  base: process.env.VITE_BASE || '/',
  plugins: [react(), csp()],
  server: {
    proxy: { '/api': { target: process.env.API_URL || 'http://localhost:8080', changeOrigin: true } },
  },
});
