# FinSight - Financial Transaction Tracker

A comprehensive financial transaction tracking system with fraud detection, subscription management, and actionable insights.

## Features

- 📊 **Dashboard** - Real-time financial overview with income, expenses, and balance tracking
- 💳 **Transaction Management** - Create, filter, and track all financial transactions
- 🚨 **Fraud Detection** - Rule-based fraud detection with risk scoring (0-100)
- 📱 **Subscription Tracking** - Automatic detection of recurring payments
- 🔔 **Fraud Alerts** - Real-time alerts for suspicious transactions
- 📈 **Analytics** - Spending trends and category-based insights

## Tech Stack

### Backend
- Java 17
- Spring Boot 3.4.3
- Spring Data JPA
- H2 Database (in-memory)
- Maven
- JUnit 5 + jqwik (property-based testing)

### Frontend
- React 18
- React Router v6
- Axios
- Modern CSS with blue/white color palette

### Deployment
- Docker & Docker Compose
- Multi-stage builds for optimization
- Health checks for reliability

## Quick Start

### Prerequisites
- Docker and Docker Compose installed
- Ports 8080 (backend) and 3000 (frontend) available

### Run with Docker

```bash
# Build and start all services
docker-compose up --build

# Or run in detached mode
docker-compose up -d --build
```

The application will be available at:
- Frontend: http://localhost:5733
- Backend API: http://localhost:8389/api
- H2 Console: http://localhost:8389/h2-console

### Run Locally (Development)

#### Backend
```bash
cd backend
mvn spring-boot:run
```

#### Frontend
```bash
cd frontend
npm install
npm start
```

## API Endpoints

### Transactions
- `POST /api/transactions` - Create transaction
- `GET /api/transactions` - List transactions (with filters)

### Dashboard
- `GET /api/summary` - Get dashboard summary

### Fraud Alerts
- `GET /api/fraud/alerts` - List fraud alerts
- `PUT /api/fraud/alerts/{id}/resolve` - Resolve alert

### Subscriptions
- `GET /api/subscriptions` - Detect and list subscriptions
- `GET /api/subscriptions/due-soon` - Get upcoming subscriptions
- `PUT /api/subscriptions/{id}/ignore` - Ignore subscription

## Fraud Detection Rules

The system uses 4 rule-based detection algorithms:

1. **High Amount Anomaly** (+30 points) - Transaction > 3x user average
2. **Rapid-Fire Activity** (+25 points) - 5+ transactions in 10 minutes
3. **Geographical Anomaly** (+25 points) - Different location < 2 hours
4. **Unusual Category** (+20 points) - Never-used category

Risk Levels:
- LOW: Score 0-39
- MEDIUM: Score 40-69
- HIGH: Score 70-100 (flagged as fraudulent)

## Demo Data

On first login, the system automatically generates 25-50 demo transactions with:
- Realistic amounts per category
- Distributed across 60-90 days
- Multiple transaction types (income/expense)
- Fraud triggers for testing

## Testing

```bash
cd backend
mvn test
```

Test Results:
- 10 tests passing
- 6 unit tests
- 4 property-based tests (100 iterations each)

## Project Structure

```
.
├── backend/
│   ├── src/
│   │   ├── main/java/com/finsight/
│   │   │   ├── controller/     # REST controllers
│   │   │   ├── service/        # Business logic
│   │   │   ├── repository/     # Data access
│   │   │   ├── model/          # Entities
│   │   │   ├── dto/            # Data transfer objects
│   │   │   └── exception/      # Error handling
│   │   └── test/               # Tests
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── components/         # Reusable components
│   │   ├── pages/              # Page components
│   │   ├── services/           # API services
│   │   └── App.js
│   ├── Dockerfile
│   ├── nginx.conf
│   └── package.json
└── docker-compose.yml
```

## Color Palette

The frontend uses a clean blue and white color scheme:
- Primary Blue: #1e40af, #3b82f6
- Success Green: #10b981
- Warning Yellow: #f59e0b
- Danger Red: #ef4444
- Neutral Grays: #f8fafc, #e2e8f0, #64748b

## License

MIT License - feel free to use this project for learning and development.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
