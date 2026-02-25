# FinSight Project Structure

## 📁 Complete Directory Tree

```
finsight/
├── 📄 README.md                          # Main documentation
├── 📄 QUICKSTART.md                      # Quick start guide
├── 📄 DEPLOYMENT.md                      # Deployment guide
├── 📄 PROJECT_STATUS.md                  # Implementation status
├── 📄 COMPLETE.md                        # Completion summary
├── 📄 docker-compose.yml                 # Docker orchestration
├── 📄 .gitignore                         # Git ignore rules
├── 📄 .dockerignore                      # Docker ignore rules
│
├── 📂 backend/                           # Spring Boot Backend
│   ├── 📄 Dockerfile                     # Backend Docker config
│   ├── 📄 pom.xml                        # Maven dependencies
│   ├── 📄 mvnw                           # Maven wrapper
│   ├── 📂 .mvn/                          # Maven wrapper files
│   ├── 📂 src/
│   │   ├── 📂 main/
│   │   │   ├── 📂 java/com/finsight/
│   │   │   │   ├── 📄 FinSightApplication.java
│   │   │   │   │
│   │   │   │   ├── 📂 controller/       # REST Controllers
│   │   │   │   │   ├── 📄 TransactionController.java
│   │   │   │   │   ├── 📄 FraudAlertController.java
│   │   │   │   │   ├── 📄 SubscriptionController.java
│   │   │   │   │   ├── 📄 DashboardController.java
│   │   │   │   │   └── 📄 HealthController.java
│   │   │   │   │
│   │   │   │   ├── 📂 service/          # Business Logic
│   │   │   │   │   ├── 📄 TransactionService.java
│   │   │   │   │   ├── 📄 FraudDetectionService.java
│   │   │   │   │   ├── 📄 FraudAlertService.java
│   │   │   │   │   ├── 📄 SubscriptionDetectorService.java
│   │   │   │   │   ├── 📄 DashboardService.java
│   │   │   │   │   ├── 📄 DemoDataService.java
│   │   │   │   │   └── 📄 AuditLogService.java
│   │   │   │   │
│   │   │   │   ├── 📂 repository/       # Data Access
│   │   │   │   │   ├── 📄 TransactionRepository.java
│   │   │   │   │   ├── 📄 UserRepository.java
│   │   │   │   │   ├── 📄 FraudAlertRepository.java
│   │   │   │   │   ├── 📄 SubscriptionRepository.java
│   │   │   │   │   └── 📄 AuditLogRepository.java
│   │   │   │   │
│   │   │   │   ├── 📂 model/            # Entities
│   │   │   │   │   ├── 📄 Transaction.java
│   │   │   │   │   ├── 📄 User.java
│   │   │   │   │   ├── 📄 FraudAlert.java
│   │   │   │   │   ├── 📄 Subscription.java
│   │   │   │   │   ├── 📄 AuditLog.java
│   │   │   │   │   ├── 📄 RiskLevel.java
│   │   │   │   │   └── 📄 SubscriptionStatus.java
│   │   │   │   │
│   │   │   │   ├── 📂 dto/              # Data Transfer Objects
│   │   │   │   │   ├── 📄 TransactionRequest.java
│   │   │   │   │   ├── 📄 TransactionResponse.java
│   │   │   │   │   ├── 📄 FraudAlertDto.java
│   │   │   │   │   ├── 📄 SubscriptionDto.java
│   │   │   │   │   ├── 📄 DashboardSummary.java
│   │   │   │   │   ├── 📄 TimeSeriesPoint.java
│   │   │   │   │   ├── 📄 FraudDetectionResult.java
│   │   │   │   │   └── 📄 ErrorResponse.java
│   │   │   │   │
│   │   │   │   └── 📂 exception/        # Error Handling
│   │   │   │       ├── 📄 GlobalExceptionHandler.java
│   │   │   │       └── 📄 ResourceNotFoundException.java
│   │   │   │
│   │   │   └── 📂 resources/
│   │   │       └── 📄 application.yml   # App configuration
│   │   │
│   │   └── 📂 test/                     # Tests
│   │       └── 📂 java/com/finsight/service/
│   │           ├── 📄 FraudDetectionServiceTest.java
│   │           └── 📄 FraudDetectionServicePropertyTest.java
│   │
│   └── 📂 target/                       # Build output (generated)
│
└── 📂 frontend/                         # React Frontend
    ├── 📄 Dockerfile                    # Frontend Docker config
    ├── 📄 nginx.conf                    # nginx configuration
    ├── 📄 package.json                  # npm dependencies
    ├── 📄 .env                          # Environment variables
    ├── 📄 .env.example                  # Environment template
    │
    ├── 📂 public/                       # Static files
    │   ├── 📄 index.html                # HTML template
    │   ├── 📄 manifest.json             # PWA manifest
    │   └── 📄 robots.txt                # SEO robots file
    │
    ├── 📂 src/                          # Source code
    │   ├── 📄 index.js                  # Entry point
    │   ├── 📄 index.css                 # Global styles
    │   ├── 📄 App.js                    # Main app component
    │   ├── 📄 App.css                   # App styles
    │   │
    │   ├── 📂 components/               # Reusable Components
    │   │   ├── 📄 Navbar.js
    │   │   ├── 📄 Navbar.css
    │   │   ├── 📄 Card.js
    │   │   ├── 📄 Card.css
    │   │   ├── 📄 Button.js
    │   │   ├── 📄 Button.css
    │   │   ├── 📄 Badge.js
    │   │   └── 📄 Badge.css
    │   │
    │   ├── 📂 pages/                    # Page Components
    │   │   ├── 📄 Dashboard.js
    │   │   ├── 📄 Dashboard.css
    │   │   ├── 📄 Transactions.js
    │   │   ├── 📄 Transactions.css
    │   │   ├── 📄 FraudAlerts.js
    │   │   ├── 📄 FraudAlerts.css
    │   │   ├── 📄 Subscriptions.js
    │   │   └── 📄 Subscriptions.css
    │   │
    │   └── 📂 services/                 # API Services
    │       └── 📄 api.js                # API client
    │
    ├── 📂 build/                        # Production build (generated)
    └── 📂 node_modules/                 # Dependencies (generated)
```

## 📊 File Count Summary

### Backend
- **Controllers**: 5 files
- **Services**: 7 files
- **Repositories**: 5 files
- **Models**: 7 files
- **DTOs**: 8 files
- **Exception Handlers**: 2 files
- **Tests**: 2 files
- **Total Backend Files**: ~36 Java files

### Frontend
- **Components**: 8 files (4 components × 2 files each)
- **Pages**: 8 files (4 pages × 2 files each)
- **Services**: 1 file
- **Config**: 5 files
- **Total Frontend Files**: ~22 files

### Configuration & Documentation
- **Docker**: 3 files (2 Dockerfiles + docker-compose.yml)
- **Documentation**: 6 markdown files
- **Config**: 3 files (.gitignore, .dockerignore, .env)
- **Total Config Files**: ~12 files

### Grand Total
**~70 files** of production-ready code and documentation!

## 🎯 Key Directories Explained

### Backend Structure

#### `/controller`
REST API endpoints that handle HTTP requests and responses.
- Maps URLs to service methods
- Validates input
- Returns formatted responses

#### `/service`
Business logic layer containing all application logic.
- Fraud detection algorithms
- Transaction processing
- Subscription detection
- Dashboard calculations

#### `/repository`
Data access layer using Spring Data JPA.
- Database queries
- Custom query methods
- JPA Specifications

#### `/model`
Entity classes representing database tables.
- JPA annotations
- Relationships
- Constraints

#### `/dto`
Data Transfer Objects for API communication.
- Request/Response objects
- Validation rules
- Clean separation from entities

### Frontend Structure

#### `/components`
Reusable UI components used across pages.
- Navbar - Navigation bar
- Card - Container component
- Button - Styled button
- Badge - Status indicators

#### `/pages`
Full page components for each route.
- Dashboard - Financial overview
- Transactions - Transaction management
- FraudAlerts - Alert management
- Subscriptions - Subscription tracking

#### `/services`
API communication layer.
- Axios configuration
- API endpoints
- Request/Response handling

## 🔄 Data Flow

```
User Action (Frontend)
    ↓
React Component
    ↓
API Service (axios)
    ↓
REST Controller (Backend)
    ↓
Service Layer (Business Logic)
    ↓
Repository (Data Access)
    ↓
Database (H2)
    ↓
Response flows back up
```

## 🎨 Styling Architecture

```
Global Styles (index.css)
    ↓
Component Styles (*.css)
    ↓
Inline Styles (when needed)
```

**Color System**:
- Primary: Blue (#1e40af, #3b82f6)
- Success: Green (#10b981)
- Warning: Yellow (#f59e0b)
- Danger: Red (#ef4444)
- Neutral: Grays (#f8fafc, #e2e8f0, #64748b)

## 🚀 Build Process

### Backend Build
```
Source Code (.java)
    ↓
Maven Compile
    ↓
Run Tests
    ↓
Package (.jar)
    ↓
Docker Image
```

### Frontend Build
```
Source Code (.js, .css)
    ↓
npm install (dependencies)
    ↓
npm build (webpack)
    ↓
Optimized Bundle
    ↓
nginx Docker Image
```

## 📦 Docker Architecture

```
docker-compose.yml
    ├── Backend Service
    │   ├── Build from backend/Dockerfile
    │   ├── Port 8080
    │   └── Health Check
    │
    └── Frontend Service
        ├── Build from frontend/Dockerfile
        ├── Port 3000
        ├── Depends on Backend
        └── Health Check
```

## 🎓 Navigation Map

```
Frontend Routes:
├── / (redirect to /dashboard)
├── /dashboard          → Dashboard.js
├── /transactions       → Transactions.js
├── /fraud-alerts       → FraudAlerts.js
└── /subscriptions      → Subscriptions.js

Backend Endpoints:
├── /api/transactions
├── /api/fraud/alerts
├── /api/subscriptions
├── /api/summary
└── /actuator/health
```

## 💡 Quick Reference

**Start Development**:
```bash
# Backend
cd backend && mvn spring-boot:run

# Frontend
cd frontend && npm start
```

**Start Production**:
```bash
docker-compose up --build
```

**Run Tests**:
```bash
cd backend && mvn test
```

**Build for Production**:
```bash
# Backend
cd backend && mvn clean package

# Frontend
cd frontend && npm run build
```

This structure provides a clean, maintainable, and scalable architecture for the FinSight application! 🎉
