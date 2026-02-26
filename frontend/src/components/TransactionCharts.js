import React, { useMemo } from 'react';
import {
  BarChart,
  Bar,
  LineChart,
  Line,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer
} from 'recharts';
import Card from './Card';
import './TransactionCharts.css';

const COLORS = {
  primary: '#3b82f6',
  success: '#10b981',
  danger: '#ef4444',
  warning: '#f59e0b',
  purple: '#8b5cf6',
  pink: '#ec4899',
  teal: '#14b8a6',
  orange: '#f97316'
};

const CATEGORY_COLORS = [
  COLORS.primary,
  COLORS.success,
  COLORS.warning,
  COLORS.purple,
  COLORS.pink,
  COLORS.teal,
  COLORS.orange,
  COLORS.danger
];

const TransactionCharts = ({ transactions }) => {
  // Process data for charts
  const chartData = useMemo(() => {
    if (!transactions || transactions.length === 0) {
      return {
        monthlyTransactions: [],
        categorySpending: [],
        fraudStatus: [],
        incomeVsExpense: []
      };
    }

    // 1. Total Transactions Per Month
    const monthlyMap = {};
    transactions.forEach(t => {
      const date = new Date(t.transactionDate);
      const monthKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
      const monthLabel = date.toLocaleDateString('en-US', { year: 'numeric', month: 'short' });
      
      if (!monthlyMap[monthKey]) {
        monthlyMap[monthKey] = { month: monthLabel, count: 0, sortKey: monthKey };
      }
      monthlyMap[monthKey].count++;
    });

    const monthlyTransactions = Object.values(monthlyMap)
      .sort((a, b) => a.sortKey.localeCompare(b.sortKey))
      .map(({ month, count }) => ({ month, count }));

    // 2. Total Transaction Amount Per Category
    const categoryMap = {};
    transactions.forEach(t => {
      if (t.type === 'EXPENSE') {
        const category = t.category || 'Uncategorized';
        categoryMap[category] = (categoryMap[category] || 0) + t.amount;
      }
    });

    const categorySpending = Object.entries(categoryMap)
      .map(([name, value]) => ({
        name: name.charAt(0).toUpperCase() + name.slice(1),
        value: parseFloat(value.toFixed(2))
      }))
      .sort((a, b) => b.value - a.value);

    // 3. Fraudulent vs Non-Fraudulent Transactions
    const fraudCount = transactions.filter(t => t.fraudulent).length;
    const nonFraudCount = transactions.length - fraudCount;

    const fraudStatus = [
      { name: 'Non-Fraudulent', value: nonFraudCount, color: COLORS.success },
      { name: 'Fraudulent', value: fraudCount, color: COLORS.danger }
    ].filter(item => item.value > 0);

    // 4. Income vs Expense Breakdown
    const incomeTotal = transactions
      .filter(t => t.type === 'INCOME')
      .reduce((sum, t) => sum + t.amount, 0);
    
    const expenseTotal = transactions
      .filter(t => t.type === 'EXPENSE')
      .reduce((sum, t) => sum + t.amount, 0);

    const incomeVsExpense = [
      { name: 'Income', value: parseFloat(incomeTotal.toFixed(2)), color: COLORS.success },
      { name: 'Expense', value: parseFloat(expenseTotal.toFixed(2)), color: COLORS.danger }
    ].filter(item => item.value > 0);

    return {
      monthlyTransactions,
      categorySpending,
      fraudStatus,
      incomeVsExpense
    };
  }, [transactions]);

  const formatCurrency = (value) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(value);
  };

  const CustomTooltip = ({ active, payload, label, valueType = 'count' }) => {
    if (active && payload && payload.length) {
      return (
        <div className="custom-tooltip">
          <p className="tooltip-label">{label || payload[0].name}</p>
          <p className="tooltip-value">
            {valueType === 'currency' 
              ? formatCurrency(payload[0].value)
              : `${payload[0].value} transaction${payload[0].value !== 1 ? 's' : ''}`
            }
          </p>
        </div>
      );
    }
    return null;
  };

  const renderCustomLabel = ({ cx, cy, midAngle, innerRadius, outerRadius, percent }) => {
    if (percent < 0.05) return null; // Don't show label if less than 5%
    
    const radius = innerRadius + (outerRadius - innerRadius) * 0.5;
    const x = cx + radius * Math.cos(-midAngle * Math.PI / 180);
    const y = cy + radius * Math.sin(-midAngle * Math.PI / 180);

    return (
      <text 
        x={x} 
        y={y} 
        fill="white" 
        textAnchor={x > cx ? 'start' : 'end'} 
        dominantBaseline="central"
        fontSize="14"
        fontWeight="600"
      >
        {`${(percent * 100).toFixed(0)}%`}
      </text>
    );
  };

  if (!transactions || transactions.length === 0) {
    return (
      <div className="charts-container">
        <Card>
          <div className="empty-state">
            No transaction data available. Add transactions to see charts.
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="charts-container">
      {/* Chart 1: Total Transactions Per Month */}
      <Card title="Total Transactions Per Month" className="chart-card">
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={chartData.monthlyTransactions}>
            <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
            <XAxis 
              dataKey="month" 
              stroke="#64748b"
              style={{ fontSize: '12px' }}
            />
            <YAxis 
              stroke="#64748b"
              style={{ fontSize: '12px' }}
              allowDecimals={false}
            />
            <Tooltip content={<CustomTooltip valueType="count" />} />
            <Line 
              type="monotone" 
              dataKey="count" 
              stroke={COLORS.primary}
              strokeWidth={3}
              dot={{ fill: COLORS.primary, r: 5 }}
              activeDot={{ r: 7 }}
              name="Transactions"
            />
          </LineChart>
        </ResponsiveContainer>
      </Card>

      {/* Chart 2: Total Transaction Amount Per Category */}
      <Card title="Total Spending Per Category" className="chart-card">
        <ResponsiveContainer width="100%" height={300}>
          {chartData.categorySpending.length > 0 ? (
            <BarChart data={chartData.categorySpending}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis 
                dataKey="name" 
                stroke="#64748b"
                style={{ fontSize: '12px' }}
                angle={-45}
                textAnchor="end"
                height={80}
              />
              <YAxis 
                stroke="#64748b"
                style={{ fontSize: '12px' }}
                tickFormatter={formatCurrency}
              />
              <Tooltip content={<CustomTooltip valueType="currency" />} />
              <Bar dataKey="value" name="Amount">
                {chartData.categorySpending.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={CATEGORY_COLORS[index % CATEGORY_COLORS.length]} />
                ))}
              </Bar>
            </BarChart>
          ) : (
            <div className="empty-state">No expense data available</div>
          )}
        </ResponsiveContainer>
      </Card>

      {/* Chart 3: Fraudulent vs Non-Fraudulent Transactions */}
      <Card title="Fraudulent vs Non-Fraudulent Transactions" className="chart-card">
        <ResponsiveContainer width="100%" height={300}>
          <PieChart>
            <Pie
              data={chartData.fraudStatus}
              cx="50%"
              cy="50%"
              labelLine={false}
              label={renderCustomLabel}
              outerRadius={100}
              fill="#8884d8"
              dataKey="value"
            >
              {chartData.fraudStatus.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={entry.color} />
              ))}
            </Pie>
            <Tooltip content={<CustomTooltip valueType="count" />} />
            <Legend 
              verticalAlign="bottom" 
              height={36}
              formatter={(value, entry) => `${value}: ${entry.payload.value}`}
            />
          </PieChart>
        </ResponsiveContainer>
      </Card>

      {/* Chart 4: Income vs Expense Breakdown */}
      <Card title="Income vs Expense Breakdown" className="chart-card">
        <ResponsiveContainer width="100%" height={300}>
          <PieChart>
            <Pie
              data={chartData.incomeVsExpense}
              cx="50%"
              cy="50%"
              labelLine={false}
              label={renderCustomLabel}
              outerRadius={100}
              fill="#8884d8"
              dataKey="value"
            >
              {chartData.incomeVsExpense.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={entry.color} />
              ))}
            </Pie>
            <Tooltip content={<CustomTooltip valueType="currency" />} />
            <Legend 
              verticalAlign="bottom" 
              height={36}
              formatter={(value, entry) => `${value}: ${formatCurrency(entry.payload.value)}`}
            />
          </PieChart>
        </ResponsiveContainer>
      </Card>
    </div>
  );
};

export default TransactionCharts;
