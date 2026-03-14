# Multi-stage build for React frontend
FROM node:20-alpine AS build
WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Build the application
RUN npm run build

# Production stage with Vite preview
FROM node:20-alpine

WORKDIR /app

# Copy package files for production dependencies
COPY package*.json ./
COPY vite.config.ts ./

# Install only production dependencies and vite
RUN npm ci --only=production && npm install vite

# Copy built app from build stage
COPY --from=build /app/dist ./dist

# Expose port
EXPOSE 5733

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:5733 || exit 1

# Start Vite preview server with proxy
CMD ["npx", "vite", "preview", "--host", "0.0.0.0", "--port", "5733"]
