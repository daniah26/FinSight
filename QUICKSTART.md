# FinSight - Quick Start Guide

## 🚀 Get Started in 3 Steps

### Step 1: Prerequisites
Make sure you have Docker and Docker Compose installed:
```bash
docker --version
docker-compose --version
```

### Step 2: Start the Application
```bash
# Clone or navigate to the project directory
cd /path/to/finsight

# Build and start all services
docker-compose up --build
```

Wait for the services to start (about 1-2 minutes). You'll see:
```
finsight-backend   | Started FinSightApplication
finsight-frontend  | nginx started
```

### Step 3: Access the Application
Open your browser and go to:
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api

## 🎯 First Time Usage

1. **Dashboard** - View your financial overview
   - The system automatically generates 25-50 demo transactions on first use
   - See total income, expenses, balance, and fraud metrics
   - View spending by category charts

2. **Transactions** - Manage your transactions
   - Click "+ Add Transaction" to create a new transaction
   - Use filters to find specific transactions
   - View fraud scores and risk levels

3. **Fraud Alerts** - Review suspicious activity
   - See all detected fraud alerts
   - Filter by severity (LOW, MEDIUM, HIGH)
   - Mark alerts as resolved

4. **Subscriptions** - Track recurring payments
   - Automatically detected from transaction patterns
   - See upcoming payments (due within 7 days)
   - Ignore subscriptions you don't want to track

## 🎨 UI Features

### Color Coding
- 💵 **Green** - Income transactions
- 💸 **Red** - Expense transactions
- 🟢 **Green Badge** - Low risk / Resolved
- 🟡 **Yellow Badge** - Medium risk / Due soon
- 🔴 **Red Badge** - High risk / Fraudulent

### Navigation
- Click any menu item in the top navigation bar
- All pages are responsive and work on mobile devices

## 🔧 Development Mode

If you want to run in development mode:

### Backend
```bash
cd backend
mvn spring-boot:run
```
Access at: http://localhost:8080

### Frontend
```bash
cd frontend
npm install
npm start
```
Access at: http://localhost:3000

## 🛑 Stop the Application

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

## 📊 Test Data

The system automatically generates demo data including:
- 25-50 transactions across 60-90 days
- Multiple categories (groceries, utilities, entertainment, etc.)
- Both income and expense transactions
- Some transactions with fraud triggers for testing

## 🐛 Troubleshooting

### Port Already in Use
If ports 3000 or 8080 are already in use:
```bash
# Check what's using the port
lsof -i :3000
lsof -i :8080

# Kill the process or change ports in docker-compose.yml
```

### Services Not Starting
```bash
# Check logs
docker-compose logs backend
docker-compose logs frontend

# Rebuild from scratch
docker-compose down -v
docker-compose up --build --force-recreate
```

### Frontend Can't Connect to Backend
- Make sure both services are running
- Check that backend is healthy: http://localhost:8080/actuator/health
- Verify CORS is enabled in backend

## 📚 API Documentation

### Create Transaction
```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "amount": 50.00,
    "type": "EXPENSE",
    "category": "groceries",
    "description": "Weekly shopping",
    "transactionDate": "2024-01-15T10:30:00"
  }'
```

### Get Dashboard Summary
```bash
curl http://localhost:8080/api/summary?userId=1
```

### Get Transactions
```bash
curl "http://localhost:8080/api/transactions?userId=1&type=EXPENSE&page=0&size=20"
```

## 🎓 Learn More

- Check `README.md` for detailed documentation
- See `PROJECT_STATUS.md` for implementation details
- Review `design.md` for architecture information

## 💡 Tips

1. **Demo Data**: The system generates demo data automatically on first use
2. **Fraud Detection**: Try creating a large transaction to trigger fraud detection
3. **Subscriptions**: Create multiple transactions to the same merchant 30 days apart
4. **Filters**: Use the filter options to find specific transactions quickly
5. **Responsive**: Try the app on your phone - it's fully responsive!

## 🎉 Enjoy FinSight!

You're all set! Explore the features and manage your financial transactions with ease.
