import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { "@": path.resolve(__dirname, "./src") },
    dedupe: ["react", "react-dom"],
  },
  server: {
    port: 5733,
    proxy: {
      '/api': {
        target: 'http://backend:8389',
        changeOrigin: true,
        secure: false,
      }
    }
  },
  preview: {
    port: 5733,
    proxy: {
      '/api': {
        target: 'http://backend:8389',
        changeOrigin: true,
        secure: false,
      }
    }
  }
});