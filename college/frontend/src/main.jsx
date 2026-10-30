import React from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, HashRouter } from 'react-router-dom';
import App from './App.jsx';
import './styles.css';

// GitHub Pages has no server-side routing: the deployed build uses #/ URLs.
const Router = import.meta.env.VITE_HASH_ROUTER === 'true' ? HashRouter : BrowserRouter;

createRoot(document.getElementById('root')).render(
  <Router>
    <App />
  </Router>,
);
