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

# List build output for debugging
RUN ls -la dist/

# Production stage with Node.js server
FROM node:20-alpine

WORKDIR /app

# Install production dependencies for the server
RUN npm init -y && npm install express http-proxy-middleware

# Copy server file
COPY server.js ./

# Copy built app from build stage
COPY --from=build /app/dist ./dist

# List files for debugging
RUN ls -la ./
RUN ls -la ./dist/

# Expose port
EXPOSE 5733

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:5733/health || exit 1

# Start the server
CMD ["node", "server.js"]
