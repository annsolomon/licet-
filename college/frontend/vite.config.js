import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The browser talks only to :5173; Vite forwards /api to Spring Boot on :8080 (no CORS trouble in Codespaces).
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: { '/api': { target: process.env.API_URL || 'http://localhost:8080', changeOrigin: true } },
  },
});
