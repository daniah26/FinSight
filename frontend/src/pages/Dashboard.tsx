import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { TrendingUp, TrendingDown, Wallet, ShieldAlert, Loader2 } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import TransactionCharts from '../components/TransactionCharts';
import { useAuth } from '../context/AuthContext';
import { getDashboardSummary, getAllTransactions } from '../lib/api';
import type { DashboardSummary, Transaction } from '../lib/mockData';

const formatCurrency = (amount: number) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);

const container = {
  hidden: {},
  show: { transition: { staggerChildren: 0.06 } },
};
const item = {
  hidden: { opacity: 0, y: 12 },
  show: { opacity: 1, y: 0, transition: { duration: 0.35 } },
};

const Dashboard = () => {
  const { user } = useAuth();
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!user?.id) return;
    const fetchData = async () => {
      try {
        setLoading(true);
        const [summaryRes, txRes] = await Promise.all([
          getDashboardSummary(user.id),
          getAllTransactions(user.id),
        ]);
        setSummary(summaryRes.data);
        const txData = txRes.data?.content || txRes.data || [];
        setTransactions(Array.isArray(txData) ? txData : []);
      } catch (err: any) {
        setError(err.response?.data?.message || 'Failed to load dashboard');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [user?.id]);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="w-8 h-8 animate-spin text-primary" />
      </div>
    );
  }

  if (error || !summary) {
    return (
      <div className="text-center py-12 text-destructive">
        <p>{error || 'No data available'}</p>
      </div>
    );
  }

  const stats = [
    {
      label: 'Total Income',
      value: formatCurrency(summary.totalIncome),
      icon: TrendingUp,
      color: 'text-success',
      bgColor: 'bg-success/10',
    },
    {
      label: 'Total Expenses',
      value: formatCurrency(summary.totalExpenses),
      icon: TrendingDown,
      color: 'text-danger',
      bgColor: 'bg-danger/10',
    },
    {
      label: 'Net Balance',
      value: formatCurrency(summary.currentBalance),
      icon: Wallet,
      color: 'text-info',
      bgColor: 'bg-info/10',
    },
    {
      label: 'Flagged Transactions',
      value: summary.totalFlaggedTransactions.toString(),
      subtext: `Avg score: ${summary.averageFraudScore.toFixed(1)}`,
      icon: ShieldAlert,
      color: 'text-warning',
      bgColor: 'bg-warning/10',
    },
  ];

  const maxCategoryAmount = Math.max(...Object.values(summary.spendingByCategory || {}), 1);

  return (
    <motion.div variants={container} initial="hidden" animate="show">
      <motion.div variants={item} className="mb-8">
        <h1 className="font-display text-2xl font-bold text-foreground tracking-tight">Overview</h1>
        <p className="text-sm text-muted-foreground mt-1">Your financial summary at a glance</p>
      </motion.div>

      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 mb-8">
        {stats.map((stat) => (
          <motion.div key={stat.label} variants={item}>
            <Card className="border-border bg-card hover:border-primary/30 transition-all hover:-translate-y-0.5">
              <CardContent className="p-5">
                <div className="flex items-start justify-between mb-4">
                  <div className={`w-10 h-10 rounded-lg ${stat.bgColor} flex items-center justify-center`}>
                    <stat.icon className={`w-5 h-5 ${stat.color}`} />
                  </div>
                </div>
                <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider mb-1">{stat.label}</p>
                <p className="font-display text-2xl font-bold text-foreground tracking-tight">{stat.value}</p>
                {stat.subtext && <p className="text-xs text-muted-foreground mt-1">{stat.subtext}</p>}
              </CardContent>
            </Card>
          </motion.div>
        ))}
      </div>

      <TransactionCharts transactions={transactions} />

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <motion.div variants={item}>
          <Card className="border-border bg-card">
            <CardHeader className="pb-4">
              <CardTitle className="text-base font-display font-semibold">Spending by Category</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              {Object.entries(summary.spendingByCategory || {})
                .sort(([, a], [, b]) => b - a)
                .map(([category, amount]) => (
                  <div key={category} className="space-y-1.5">
                    <div className="flex justify-between items-center">
                      <span className="text-sm text-muted-foreground capitalize">{category}</span>
                      <span className="text-sm font-bold font-display text-foreground">{formatCurrency(amount)}</span>
                    </div>
                    <div className="h-1.5 bg-muted rounded-full overflow-hidden">
                      <motion.div
                        className="h-full rounded-full bg-gradient-to-r from-primary to-accent"
                        initial={{ width: 0 }}
                        animate={{ width: `${(amount / maxCategoryAmount) * 100}%` }}
                        transition={{ duration: 0.8, ease: 'easeOut' }}
                      />
                    </div>
                  </div>
                ))}
            </CardContent>
          </Card>
        </motion.div>

        <motion.div variants={item}>
          <Card className="border-border bg-card">
            <CardHeader className="pb-4">
              <CardTitle className="text-base font-display font-semibold">Fraud by Category</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              {Object.entries(summary.fraudByCategory || {}).length > 0
                ? Object.entries(summary.fraudByCategory).map(([category, count]) => (
                    <div
                      key={category}
                      className="flex justify-between items-center p-3 bg-warning/10 border border-warning/20 rounded-lg"
                    >
                      <span className="text-sm font-semibold text-warning capitalize">{category}</span>
                      <span className="text-sm font-bold text-warning">{count} incidents</span>
                    </div>
                  ))
                : <p className="text-sm text-muted-foreground text-center py-6">No fraud incidents detected</p>}
            </CardContent>
          </Card>
        </motion.div>

        <motion.div variants={item} className="lg:col-span-2">
          <Card className="border-border bg-card">
            <CardHeader className="pb-4">
              <CardTitle className="text-base font-display font-semibold">Recent Transactions</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {transactions.slice(0, 6).map((tx) => (
                <div
                  key={tx.id}
                  className={`flex items-center justify-between p-3 rounded-lg transition-colors hover:bg-muted/50 ${
                    tx.fraudulent ? 'border border-danger/20 bg-danger/5' : ''
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div className={`w-9 h-9 rounded-lg flex items-center justify-center text-sm ${
                      tx.type === 'INCOME' ? 'bg-success/10 text-success' : 'bg-danger/10 text-danger'
                    }`}>
                      {tx.type === 'INCOME' ? '↑' : '↓'}
                    </div>
                    <div>
                      <p className="text-sm font-medium text-foreground capitalize">{tx.category}</p>
                      <p className="text-xs text-muted-foreground">{tx.description}</p>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className={`text-sm font-bold font-display ${
                      tx.type === 'INCOME' ? 'text-success' : 'text-foreground'
                    }`}>
                      {tx.type === 'INCOME' ? '+' : '-'}{formatCurrency(tx.amount)}
                    </p>
                    {tx.fraudulent && (
                      <span className="text-[10px] font-bold text-danger uppercase">Fraud: {tx.fraudScore}</span>
                    )}
                  </div>
                </div>
              ))}
              {transactions.length === 0 && (
                <p className="text-sm text-muted-foreground text-center py-6">No transactions yet</p>
              )}
            </CardContent>
          </Card>
        </motion.div>
      </div>
    </motion.div>
  );
};

export default Dashboard;
