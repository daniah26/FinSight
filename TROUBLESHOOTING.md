# FinSight Troubleshooting Guide

## 🔧 Common Issues and Solutions

### Issue: Backend Container Fails to Start

**Symptoms:**
```
finsight-backend exited with code 1
dependency failed to start: container finsight-backend is unhealthy
```

**Solutions:**

1. **Clean and Rebuild**
```bash
# Stop all containers
docker-compose down -v

# Remove old images
docker rmi finsight-backend finsight-frontend

# Rebuild from scratch
docker-compose up --build
```

2. **Check Backend Logs**
```bash
docker-compose logs backend
```

3. **Increase Startup Time**
The docker-compose.yml has been configured with:
- `start_period: 60s` - Gives backend 60 seconds to start
- `retries: 10` - Tries health check 10 times
- `interval: 10s` - Checks every 10 seconds

4. **Run Backend Locally First**
```bash
cd backend
mvn clean package
mvn spring-boot:run
```

If it works locally, the issue is Docker-specific.

### Issue: Port Already in Use

**Symptoms:**
```
Error: bind: address already in use
```

**Solutions:**

1. **Find and Kill Process**
```bash
# Find process on port 8080
lsof -i :8080

# Kill it
kill -9 <PID>

# Or for port 3000
lsof -i :3000
kill -9 <PID>
```

2. **Change Ports in docker-compose.yml**
```yaml
services:
  backend:
    ports:
      - "8081:8080"  # Change 8080 to 8081
  
  frontend:
    ports:
      - "3001:3000"  # Change 3000 to 3001
```

### Issue: Frontend Can't Connect to Backend

**Symptoms:**
- Frontend loads but shows "Failed to load data"
- Network errors in browser console

**Solutions:**

1. **Check Backend is Running**
```bash
curl http://localhost:8080/actuator/health
```

Should return: `{"status":"UP",...}`

2. **Check CORS Configuration**
The backend has CORS enabled for `http://localhost:3000`

3. **Verify Environment Variable**
Check `frontend/.env`:
```
REACT_APP_API_URL=http://localhost:8080/api
```

4. **Check Docker Network**
```bash
docker network inspect finsight_finsight-network
```

### Issue: "scale has no meaning for floating point numbers"

**Symptoms:**
```
finsight-backend exited with code 1
Error: scale has no meaning for floating point numbers
```

**Root Cause:**
JPA `@Column` annotation's `precision` and `scale` attributes only apply to `BigDecimal` fields, not `Double` or `Float` fields.

**Solution:**
Remove `precision` and `scale` from `@Column` annotations on Double/Float fields:
```java
// WRONG
@Column(precision = 5, scale = 2)
private Double fraudScore;

// CORRECT
@Column
private Double fraudScore;
```

This has been fixed in `Transaction.java`.

### Issue: Spring Security Blocking Health Checks

**Symptoms:**
```
Health check returns 401 Unauthorized
Container marked as unhealthy
```

**Root Cause:**
Spring Security dependency was present but no SecurityConfig existed, causing default security to block all endpoints.

**Solution:**
Created `SecurityConfig.java` to disable CSRF and permit all requests:
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
```

Note: This is appropriate for demo applications. Production apps should implement proper authentication.

### Issue: Database Errors

**Symptoms:**
```
org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
```

**Solutions:**

1. **Check data.sql**
Ensure `backend/src/main/resources/data.sql` exists with:
```sql
INSERT INTO users (id, username, email, password, created_at) 
VALUES (1, 'demo', 'demo@finsight.com', '$2a$10$...', CURRENT_TIMESTAMP);

INSERT INTO user_roles (user_id, role) VALUES (1, 'USER');
```

2. **Check application.yml**
Ensure these settings:
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop
    defer-datasource-initialization: true
  sql:
    init:
      mode: always
```

3. **Clear H2 Database**
```bash
docker-compose down -v  # -v removes volumes
docker-compose up --build
```

### Issue: Maven Build Fails

**Symptoms:**
```
Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin
```

**Solutions:**

1. **Check Java Version**
```bash
java -version  # Should be 17+
```

2. **Clean Maven Cache**
```bash
cd backend
mvn clean
rm -rf ~/.m2/repository
mvn clean package
```

3. **Check pom.xml**
Ensure Java 17 is specified:
```xml
<properties>
    <java.version>17</java.version>
</properties>
```

### Issue: Frontend Build Fails

**Symptoms:**
```
npm ERR! code ELIFECYCLE
```

**Solutions:**

1. **Clear npm Cache**
```bash
cd frontend
rm -rf node_modules package-lock.json
npm cache clean --force
npm install
```

2. **Check Node Version**
```bash
node -v  # Should be 18+
npm -v
```

3. **Build Locally First**
```bash
cd frontend
npm install
npm run build
```

### Issue: Health Check Failing

**Symptoms:**
```
Health check failed
```

**Solutions:**

1. **Test Health Endpoint Manually**
```bash
# From host
curl http://localhost:8080/actuator/health

# From inside container
docker exec finsight-backend curl http://localhost:8080/actuator/health
```

2. **Check if curl is Installed**
```bash
docker exec finsight-backend which curl
```

3. **Increase Health Check Timeout**
In docker-compose.yml:
```yaml
healthcheck:
  start_period: 120s  # Increase from 60s
  retries: 20         # Increase from 10
```

### Issue: Out of Memory

**Symptoms:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Solutions:**

1. **Increase Docker Memory**
Docker Desktop > Settings > Resources > Memory: 4GB+

2. **Adjust Java Heap**
In docker-compose.yml:
```yaml
environment:
  - JAVA_OPTS=-Xmx1g -Xms512m
```

3. **Check Docker Stats**
```bash
docker stats
```

## 🚀 Quick Fixes

### Complete Reset
```bash
# Nuclear option - clean everything
docker-compose down -v
docker system prune -a
docker volume prune
docker-compose up --build
```

### Check Everything is Working
```bash
# 1. Check containers
docker-compose ps

# 2. Check backend health
curl http://localhost:8080/actuator/health

# 3. Check frontend
curl http://localhost:3000

# 4. Check logs
docker-compose logs -f
```

### Run Without Docker
```bash
# Terminal 1 - Backend
cd backend
mvn spring-boot:run

# Terminal 2 - Frontend
cd frontend
npm install
npm start
```

## 📊 Debugging Commands

```bash
# View all logs
docker-compose logs -f

# View backend logs only
docker-compose logs -f backend

# View last 100 lines
docker-compose logs --tail=100 backend

# Check container status
docker-compose ps

# Inspect container
docker inspect finsight-backend

# Enter container shell
docker exec -it finsight-backend sh

# Check network
docker network ls
docker network inspect finsight_finsight-network

# Check volumes
docker volume ls

# Check images
docker images | grep finsight
```

## 🆘 Still Having Issues?

1. **Check System Requirements**
   - Docker 20.10+
   - Docker Compose 2.0+
   - 4GB RAM minimum
   - 10GB disk space

2. **Verify Files Exist**
   ```bash
   ls -la backend/src/main/resources/
   ls -la frontend/src/
   ```

3. **Check File Permissions**
   ```bash
   chmod +x backend/mvnw
   ```

4. **Try Local Development**
   Run backend and frontend locally without Docker to isolate the issue.

5. **Check Docker Daemon**
   ```bash
   docker info
   docker version
   ```

## 💡 Prevention Tips

1. **Always use clean builds**
   ```bash
   docker-compose down -v
   docker-compose up --build
   ```

2. **Monitor resources**
   ```bash
   docker stats
   ```

3. **Keep Docker updated**
   ```bash
   docker --version
   docker-compose --version
   ```

4. **Check logs regularly**
   ```bash
   docker-compose logs -f
   ```

## 📞 Getting Help

If you're still stuck:
1. Check the logs: `docker-compose logs -f`
2. Try running locally without Docker
3. Verify all files are present
4. Check system resources
5. Review error messages carefully

Most issues are related to:
- Port conflicts
- Memory limits
- Missing dependencies
- File permissions
- Network configuration

Good luck! 🚀
