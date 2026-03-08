import { useState, useMemo, useEffect } from 'react';
import { motion } from 'framer-motion';
import { getFraudAlerts, resolveAlert } from '../lib/api';
import { useAuth } from '../context/AuthContext';
import type { FraudAlert } from '../lib/mockData';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Loader2 } from 'lucide-react';

const formatCurrency = (amount: number) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);

const formatDate = (dateString: string) =>
  new Date(dateString).toLocaleDateString('en-US', {
    month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  });

const severityIcon: Record<string, string> = { LOW: '🟡', MEDIUM: '🟠', HIGH: '🔴' };
const severityBadgeClass: Record<string, string> = {
  LOW: 'bg-warning/10 text-warning border-warning/20',
  MEDIUM: 'bg-warning/20 text-warning border-warning/30',
  HIGH: 'bg-danger/10 text-danger border-danger/20',
};

const container = { hidden: {}, show: { transition: { staggerChildren: 0.05 } } };
const item = { hidden: { opacity: 0, y: 8 }, show: { opacity: 1, y: 0 } };

const FraudAlerts = () => {
  const { user } = useAuth();
  const [alerts, setAlerts] = useState<FraudAlert[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState({ resolved: '', severity: '' });

  useEffect(() => {
    if (!user?.id) return;
    const fetchAlerts = async () => {
      try {
        setLoading(true);
        const res = await getFraudAlerts(user.id);
        const data = res.data?.content || res.data || [];
        setAlerts(Array.isArray(data) ? data : []);
      } catch (err: any) {
        setError(err.response?.data?.message || 'Failed to load alerts');
      } finally {
        setLoading(false);
      }
    };
    fetchAlerts();
  }, [user?.id]);

  const filtered = useMemo(() => {
    let result = [...alerts];
    if (filter.resolved !== '') {
      const isResolved = filter.resolved === 'true';
      result = result.filter(a => a.resolved === isResolved);
    }
    if (filter.severity) result = result.filter(a => a.severity === filter.severity);
    return result.sort((a, b) => {
      if (a.resolved !== b.resolved) return a.resolved ? 1 : -1;
      return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
    });
  }, [alerts, filter]);

  const handleResolve = async (alertId: number) => {
    if (!user?.id) return;
    try {
      await resolveAlert(alertId, user.id);
      setAlerts(prev => prev.map(a => a.id === alertId ? { ...a, resolved: true } : a));
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to resolve alert');
    }
  };

  const unresolvedCount = alerts.filter(a => !a.resolved).length;

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="w-8 h-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <motion.div variants={container} initial="hidden" animate="show">
      <motion.div variants={item} className="mb-8">
        <h1 className="font-display text-2xl font-bold text-foreground tracking-tight">Fraud Alerts</h1>
        <p className="text-sm text-muted-foreground mt-1">{unresolvedCount} unresolved alerts</p>
      </motion.div>

      {error && (
        <div className="mb-4 p-3 bg-destructive/10 text-destructive rounded-lg text-sm">{error}</div>
      )}

      <motion.div variants={item}>
        <Card className="border-border bg-card mb-6">
          <CardContent className="p-4">
            <div className="flex flex-wrap items-center gap-3">
              <select
                value={filter.resolved}
                onChange={(e) => setFilter({ ...filter, resolved: e.target.value })}
                className="px-3 py-2 bg-input border border-border rounded-lg text-foreground text-sm focus:outline-none focus:border-primary transition-all"
              >
                <option value="">All Alerts</option>
                <option value="false">Unresolved</option>
                <option value="true">Resolved</option>
              </select>
              <select
                value={filter.severity}
                onChange={(e) => setFilter({ ...filter, severity: e.target.value })}
                className="px-3 py-2 bg-input border border-border rounded-lg text-foreground text-sm focus:outline-none focus:border-primary transition-all"
              >
                <option value="">All Severities</option>
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
              </select>
              <button
                onClick={() => setFilter({ resolved: '', severity: '' })}
                className="px-3 py-2 bg-muted text-muted-foreground rounded-lg text-sm font-medium hover:text-foreground transition-all"
              >
                Clear
              </button>
            </div>
          </CardContent>
        </Card>
      </motion.div>

      <motion.div variants={item}>
        <Card className="border-border bg-card">
          <CardHeader className="pb-4">
            <CardTitle className="text-base font-display font-semibold">Alerts ({filtered.length})</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {filtered.length === 0 ? (
              <p className="text-sm text-muted-foreground text-center py-8">No fraud alerts found</p>
            ) : (
              filtered.map((alert) => (
                <div
                  key={alert.id}
                  className={`p-4 rounded-xl border transition-all hover:-translate-y-0.5 ${
                    alert.resolved
                      ? 'border-success/20 bg-success/5 opacity-70'
                      : 'border-danger/20 bg-danger/5'
                  }`}
                >
                  <div className="flex items-start gap-3">
                    <span className="text-lg mt-0.5">{severityIcon[alert.severity] || '⚠️'}</span>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-1 flex-wrap">
                        <span className="text-sm font-semibold text-foreground">{alert.message}</span>
                        <Badge variant="outline" className={`text-[10px] h-5 ${severityBadgeClass[alert.severity]}`}>
                          {alert.severity}
                        </Badge>
                        {alert.resolved && (
                          <Badge variant="outline" className="text-[10px] h-5 bg-success/10 text-success border-success/20">
                            Resolved
                          </Badge>
                        )}
                      </div>

                      <div className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted-foreground mt-2">
                        <span>
                          <span className="text-muted-foreground/60">Category:</span>{' '}
                          <span className="capitalize">{alert.transaction.category}</span>
                        </span>
                        <span>
                          <span className="text-muted-foreground/60">Amount:</span>{' '}
                          {formatCurrency(alert.transaction.amount)}
                        </span>
                        <span>
                          <span className="text-muted-foreground/60">Score:</span>{' '}
                          {alert.transaction.fraudScore}/100
                        </span>
                        <span>
                          <span className="text-muted-foreground/60">Flagged:</span>{' '}
                          {formatDate(alert.createdAt)}
                        </span>
                      </div>
                    </div>

                    {!alert.resolved && (
                      <button
                        onClick={() => handleResolve(alert.id)}
                        className="px-3 py-1.5 bg-success/10 text-success border border-success/20 rounded-lg text-xs font-semibold hover:bg-success/20 transition-all shrink-0"
                      >
                        Resolve
                      </button>
                    )}
                  </div>
                </div>
              ))
            )}
          </CardContent>
        </Card>
      </motion.div>
    </motion.div>
  );
};

export default FraudAlerts;
