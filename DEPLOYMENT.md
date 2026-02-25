# FinSight Deployment Guide

## 🐳 Docker Deployment (Recommended)

### Prerequisites
- Docker 20.10+
- Docker Compose 2.0+
- 2GB RAM minimum
- 5GB disk space

### Quick Deploy

```bash
# 1. Clone/navigate to project
cd finsight

# 2. Build and start
docker-compose up --build -d

# 3. Check status
docker-compose ps

# 4. View logs
docker-compose logs -f
```

### Service URLs
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api
- Health Check: http://localhost:8080/actuator/health
- H2 Console: http://localhost:8080/h2-console

### Docker Commands

```bash
# Start services
docker-compose up -d

# Stop services
docker-compose down

# Restart services
docker-compose restart

# View logs
docker-compose logs -f [service-name]

# Rebuild specific service
docker-compose up -d --build backend

# Clean everything
docker-compose down -v --rmi all
```

## 🔧 Manual Deployment

### Backend Deployment

#### Build
```bash
cd backend
mvn clean package -DskipTests
```

#### Run
```bash
java -jar target/finsight-backend-1.0.0.jar
```

#### Configuration
Environment variables:
```bash
export SPRING_PROFILES_ACTIVE=production
export SERVER_PORT=8080
export SPRING_DATASOURCE_URL=jdbc:h2:mem:finsight
```

### Frontend Deployment

#### Build
```bash
cd frontend
npm install
npm run build
```

#### Serve with nginx
```bash
# Copy build to nginx
cp -r build/* /usr/share/nginx/html/

# Or use serve
npx serve -s build -l 3000
```

## 🌐 Production Deployment

### AWS EC2 Deployment

1. **Launch EC2 Instance**
   - Ubuntu 22.04 LTS
   - t2.medium or larger
   - Open ports: 80, 443, 8080, 3000

2. **Install Docker**
```bash
sudo apt update
sudo apt install docker.io docker-compose -y
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker $USER
```

3. **Deploy Application**
```bash
git clone <repository>
cd finsight
docker-compose up -d --build
```

4. **Setup nginx Reverse Proxy**
```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }

    location /api {
        proxy_pass http://localhost:8080/api;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### Docker Hub Deployment

1. **Build and Tag Images**
```bash
# Backend
docker build -t yourusername/finsight-backend:latest ./backend
docker push yourusername/finsight-backend:latest

# Frontend
docker build -t yourusername/finsight-frontend:latest ./frontend
docker push yourusername/finsight-frontend:latest
```

2. **Update docker-compose.yml**
```yaml
services:
  backend:
    image: yourusername/finsight-backend:latest
    # ... rest of config

  frontend:
    image: yourusername/finsight-frontend:latest
    # ... rest of config
```

### Kubernetes Deployment

1. **Create Deployment Files**

`backend-deployment.yaml`:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: finsight-backend
spec:
  replicas: 2
  selector:
    matchLabels:
      app: finsight-backend
  template:
    metadata:
      labels:
        app: finsight-backend
    spec:
      containers:
      - name: backend
        image: yourusername/finsight-backend:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
---
apiVersion: v1
kind: Service
metadata:
  name: finsight-backend
spec:
  selector:
    app: finsight-backend
  ports:
  - port: 8080
    targetPort: 8080
  type: LoadBalancer
```

2. **Deploy**
```bash
kubectl apply -f backend-deployment.yaml
kubectl apply -f frontend-deployment.yaml
```

## 🔒 Security Considerations

### Production Checklist

- [ ] Change default H2 database to PostgreSQL/MySQL
- [ ] Enable HTTPS with SSL certificates
- [ ] Set up authentication (JWT tokens)
- [ ] Configure CORS properly
- [ ] Enable rate limiting
- [ ] Set up monitoring and logging
- [ ] Configure backup strategy
- [ ] Use secrets management
- [ ] Enable firewall rules
- [ ] Set up health checks

### Environment Variables

**Backend**:
```bash
SPRING_PROFILES_ACTIVE=production
SERVER_PORT=8080
SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/finsight
SPRING_DATASOURCE_USERNAME=finsight_user
SPRING_DATASOURCE_PASSWORD=secure_password
JWT_SECRET=your_jwt_secret_key
CORS_ALLOWED_ORIGINS=https://yourdomain.com
```

**Frontend**:
```bash
REACT_APP_API_URL=https://api.yourdomain.com
REACT_APP_ENV=production
```

## 📊 Monitoring

### Health Checks

```bash
# Backend health
curl http://localhost:8080/actuator/health

# Frontend health
curl http://localhost:3000

# Docker health
docker-compose ps
```

### Logs

```bash
# View all logs
docker-compose logs -f

# Backend logs only
docker-compose logs -f backend

# Frontend logs only
docker-compose logs -f frontend

# Last 100 lines
docker-compose logs --tail=100
```

### Metrics

Add Spring Boot Actuator endpoints:
- `/actuator/metrics` - Application metrics
- `/actuator/health` - Health status
- `/actuator/info` - Application info

## 🔄 Updates and Maintenance

### Update Application

```bash
# Pull latest changes
git pull origin main

# Rebuild and restart
docker-compose down
docker-compose up -d --build

# Or rolling update
docker-compose up -d --no-deps --build backend
docker-compose up -d --no-deps --build frontend
```

### Database Backup

```bash
# Backup H2 database
docker exec finsight-backend \
  java -cp /app/app.jar org.h2.tools.Script \
  -url jdbc:h2:mem:finsight \
  -user sa \
  -script backup.sql

# Restore
docker exec finsight-backend \
  java -cp /app/app.jar org.h2.tools.RunScript \
  -url jdbc:h2:mem:finsight \
  -user sa \
  -script backup.sql
```

## 🐛 Troubleshooting

### Common Issues

**Port conflicts**:
```bash
# Find process using port
lsof -i :8080
lsof -i :3000

# Kill process
kill -9 <PID>
```

**Container won't start**:
```bash
# Check logs
docker-compose logs backend

# Rebuild from scratch
docker-compose down -v
docker-compose build --no-cache
docker-compose up -d
```

**Out of memory**:
```bash
# Increase Docker memory limit
# Docker Desktop > Settings > Resources > Memory

# Or add to docker-compose.yml
services:
  backend:
    mem_limit: 1g
```

**Network issues**:
```bash
# Recreate network
docker-compose down
docker network prune
docker-compose up -d
```

## 📈 Scaling

### Horizontal Scaling

```yaml
services:
  backend:
    deploy:
      replicas: 3
    # ... rest of config
```

### Load Balancing

Use nginx as load balancer:
```nginx
upstream backend {
    server backend1:8080;
    server backend2:8080;
    server backend3:8080;
}

server {
    location /api {
        proxy_pass http://backend;
    }
}
```

## 🎯 Performance Optimization

### Backend
- Enable caching
- Use connection pooling
- Optimize database queries
- Enable compression

### Frontend
- Enable gzip compression (already configured)
- Use CDN for static assets
- Implement lazy loading
- Optimize images

### Docker
- Use multi-stage builds (already implemented)
- Minimize layer count
- Use .dockerignore (already configured)
- Cache dependencies

## 📞 Support

For issues or questions:
1. Check logs: `docker-compose logs -f`
2. Review health checks
3. Check GitHub issues
4. Contact support team

## 🎉 Success!

Your FinSight application is now deployed and ready to use!
