import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';

// One code base, four separate web apps. PORTAL=hod|parent|advisor|student picks the app, its dev port and its API service.
const PORTALS = {
  hod: { port: 5174, api: 8082 },
  parent: { port: 5175, api: 8083 },
  advisor: { port: 5176, api: 8084 },
  student: { port: 5177, api: 8086 },
};
const portal = process.env.PORTAL || 'hod';
const cfg = PORTALS[portal];
if (!cfg) throw new Error(`Unknown PORTAL "${portal}" (use hod, parent or advisor)`);

export default defineConfig({
  root: path.resolve(__dirname, 'src', portal),
  plugins: [react()],
  resolve: { alias: { '@shared': path.resolve(__dirname, 'src/shared') } },
  server: {
    port: cfg.port,
    strictPort: true,
    fs: { allow: [__dirname] },   // src/shared lives outside the portal's root folder
    proxy: { '/api': { target: process.env.API_URL || `http://localhost:${cfg.api}`, changeOrigin: true } },
  },
  preview: { port: cfg.port, proxy: { '/api': { target: `http://localhost:${cfg.api}`, changeOrigin: true } } },
  build: { outDir: path.resolve(__dirname, 'dist', portal), emptyOutDir: true },
});
