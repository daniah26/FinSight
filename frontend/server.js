const express = require('express');
const { createProxyMiddleware } = require('http-proxy-middleware');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 5733;

// API proxy to backend — must come BEFORE static files
app.use('/api', createProxyMiddleware({
  target: 'http://backend:8389',
  changeOrigin: true,
  on: {
    error: (err, req, res) => {
      console.error('Proxy error:', err.message);
      res.status(502).json({ error: 'Backend unavailable' });
    }
  }
}));

// Also proxy /actuator (health checks etc.)
app.use('/actuator', createProxyMiddleware({
  target: 'http://backend:8389',
  changeOrigin: true,
}));

// Serve static files from dist directory
app.use(express.static(path.join(__dirname, 'dist')));

// Handle React Router — Express 5 requires explicit wildcard syntax
app.get('/{*path}', (req, res) => {
  res.sendFile(path.join(__dirname, 'dist', 'index.html'));
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Server running on port ${PORT}`);
});