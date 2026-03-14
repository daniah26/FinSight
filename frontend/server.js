const express = require('express');
const { createProxyMiddleware } = require('http-proxy-middleware');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 5733;

// Serve static files from dist directory
app.use(express.static(path.join(__dirname, 'dist')));

// API proxy to backend
app.use('/api', createProxyMiddleware({
  target: 'http://backend:8389',
  changeOrigin: true,
  pathRewrite: {
    '^/api': '/api', // keep the /api prefix
  },
}));

// Handle React Router (return index.html for all non-API routes)
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'dist', 'index.html'));
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Server running on port ${PORT}`);
});