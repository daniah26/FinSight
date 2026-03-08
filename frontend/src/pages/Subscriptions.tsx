import { useState, useMemo, useEffect } from 'react';
import { motion } from 'framer-motion';
import { getSubscriptions, ignoreSubscription, detectSubscriptions, createSubscription } from '../lib/api';
import { useAuth } from '../context/AuthContext';
import type { Subscription } from '../lib/mockData';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Clock, Plus, Loader2, Search } from 'lucide-react';

const formatCurrency = (amount: number) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);

const formatDate = (dateStr: string) => {
  // Handle both ISO datetime (2024-01-15T00:00:00) and LocalDate (2024-01-15)
  const dateOnly = dateStr.split('T')[0];
  const [y, m, d] = dateOnly.split('-').map(Number);
  return new Date(y, m - 1, d).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
};

const getDaysUntil = (dateStr: string) => {
  // Handle both ISO datetime and LocalDate
  const dateOnly = dateStr.split('T')[0];
  const [y, m, d] = dateOnly.split('-').map(Number);
  const due = new Date(y, m - 1, d);
  due.setHours(0, 0, 0, 0);
  const now = new Date();
  now.setHours(0, 0, 0, 0);
  return Math.ceil((due.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
};

const container = { hidden: {}, show: { transition: { staggerChildren: 0.05 } } };
const item = { hidden: { opacity: 0, y: 8 }, show: { opacity: 1, y: 0 } };

const Subscriptions = () => {
  const { user } = useAuth();
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showAddModal, setShowAddModal] = useState(false);
  const [confirmIgnore, setConfirmIgnore] = useState<Subscription | null>(null);
  const [detecting, setDetecting] = useState(false);
  const [addForm, setAddForm] = useState({
    merchant: '',
    avgAmount: '',
    lastPaidDate: '',
    nextDueDate: ''
  });
  const [submitting, setSubmitting] = useState(false);
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (!user?.id) return;
    const fetchSubscriptions = async () => {
      try {
        setLoading(true);
        setError('');
        console.log('Auto-detecting subscriptions for user:', user.id);
        // Auto-detect subscriptions on load
        const detectRes = await detectSubscriptions(user.id);
        console.log('Detection response:', detectRes.data);
        
        const res = await getSubscriptions(user.id);
        console.log('Get subscriptions response:', res.data);
        
        const data = res.data?.content || res.data || [];
        console.log('Processed subscriptions:', data);
        setSubscriptions(Array.isArray(data) ? data : []);
      } catch (err: any) {
        console.error('Failed to load subscriptions:', err);
        setError(err.response?.data?.message || 'Failed to load subscriptions');
        setSubscriptions([]);
      } finally {
        setLoading(false);
      }
    };
    fetchSubscriptions();
  }, [user?.id]);

  const activeSubs = useMemo(() => subscriptions.filter(s => s.status !== 'IGNORED'), [subscriptions]);
  const dueSoon = useMemo(() => activeSubs.filter(s => {
    const days = getDaysUntil(s.nextDueDate);
    return days >= 0 && days <= 7;
  }), [activeSubs]);
  const pastDue = useMemo(() => activeSubs.filter(s => {
    const days = getDaysUntil(s.nextDueDate);
    return days < 0;
  }), [activeSubs]);

  const handleIgnore = (sub: Subscription) => setConfirmIgnore(sub);
  const confirmIgnoreAction = async () => {
    if (!confirmIgnore || !user?.id) return;
    try {
      await ignoreSubscription(confirmIgnore.id, user.id);
      setSubscriptions(prev => prev.map(s => s.id === confirmIgnore.id ? { ...s, status: 'IGNORED' as const } : s));
      setConfirmIgnore(null);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to ignore subscription');
    }
  };

  const handleDetect = async () => {
    if (!user?.id) return;
    try {
      setDetecting(true);
      setError('');
      // Detect subscriptions
      await detectSubscriptions(user.id);
      // Fetch updated list
      const res = await getSubscriptions(user.id);
      const data = res.data?.content || res.data || [];
      setSubscriptions(Array.isArray(data) ? data : []);
    } catch (err: any) {
      console.error('Detection error:', err);
      setError(err.response?.data?.message || 'Failed to detect subscriptions');
    } finally {
      setDetecting(false);
    }
  };

  const handleAddSubscription = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user?.id) return;
    
    // Clear previous errors
    setFormErrors({});
    setError('');
    
    // Client-side validation
    const errors: Record<string, string> = {};
    const amount = parseFloat(addForm.avgAmount);
    const lastPaid = new Date(addForm.lastPaidDate);
    const nextDue = new Date(addForm.nextDueDate);
    const now = new Date();
    now.setHours(0, 0, 0, 0);
    
    // Merchant validation
    if (!addForm.merchant.trim()) {
      errors.merchant = 'Subscription name is required';
    } else if (addForm.merchant.length > 100) {
      errors.merchant = 'Subscription name must not exceed 100 characters';
    }
    
    // Amount validation
    if (isNaN(amount) || amount <= 0) {
      errors.avgAmount = 'Amount must be greater than $0';
    } else if (amount > 10000) {
      errors.avgAmount = 'Amount cannot exceed $10,000';
    }
    
    // Last paid date validation
    if (!addForm.lastPaidDate) {
      errors.lastPaidDate = 'Last paid date is required';
    } else if (lastPaid > now) {
      errors.lastPaidDate = 'Last paid date cannot be in the future';
    }
    
    // Next due date validation
    if (!addForm.nextDueDate) {
      errors.nextDueDate = 'Next due date is required';
    } else if (nextDue < now) {
      errors.nextDueDate = 'Next due date cannot be in the past';
    }
    
    // Date range validation (25-35 days)
    if (addForm.lastPaidDate && addForm.nextDueDate && !errors.lastPaidDate && !errors.nextDueDate) {
      const daysBetween = Math.floor((nextDue.getTime() - lastPaid.getTime()) / (1000 * 60 * 60 * 24));
      
      if (daysBetween < 25) {
        errors.nextDueDate = 'Next due date must be at least 25 days after last paid date';
      } else if (daysBetween > 35) {
        errors.nextDueDate = 'Next due date must be at most 35 days after last paid date';
      }
    }
    
    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }
    
    try {
      setSubmitting(true);
      
      await createSubscription(user.id, {
        merchant: addForm.merchant.trim(),
        avgAmount: amount,
        lastPaidDate: addForm.lastPaidDate,
        nextDueDate: addForm.nextDueDate
      });
      
      // Refresh subscriptions list
      const res = await getSubscriptions(user.id);
      const data = res.data?.content || res.data || [];
      setSubscriptions(Array.isArray(data) ? data : []);
      
      // Reset form and close modal
      setAddForm({ merchant: '', avgAmount: '', lastPaidDate: '', nextDueDate: '' });
      setFormErrors({});
      setShowAddModal(false);
    } catch (err: any) {
      console.error('Failed to add subscription:', err);
      // Handle backend validation errors
      if (err.response?.data?.errors) {
        const backendErrors: Record<string, string> = {};
        err.response.data.errors.forEach((error: any) => {
          if (error.field) {
            backendErrors[error.field] = error.message;
          }
        });
        setFormErrors(backendErrors);
      } else {
        setError(err.response?.data?.message || 'Failed to add subscription');
      }
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="w-8 h-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <motion.div variants={container} initial="hidden" animate="show">
      <motion.div variants={item} className="flex items-start justify-between mb-8">
        <div>
          <h1 className="font-display text-2xl font-bold text-foreground tracking-tight">Subscriptions</h1>
          <p className="text-sm text-muted-foreground mt-1">
            {activeSubs.length} active recurring payments
          </p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={handleDetect}
            disabled={detecting}
            className="flex items-center gap-2 px-4 py-2.5 bg-secondary text-secondary-foreground rounded-lg text-sm font-semibold hover:opacity-90 transition-all disabled:opacity-50"
          >
            {detecting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Search className="w-4 h-4" />}
            Refresh
          </button>
          <button
            onClick={() => setShowAddModal(true)}
            className="flex items-center gap-2 px-4 py-2.5 bg-primary text-primary-foreground rounded-lg text-sm font-semibold hover:bg-accent transition-all"
          >
            <Plus className="w-4 h-4" />
            Add
          </button>
        </div>
      </motion.div>

      {error && (
        <div className="mb-4 p-3 bg-destructive/10 text-destructive rounded-lg text-sm">{error}</div>
      )}

      {/* Confirm Ignore Modal */}
      {confirmIgnore && (
        <div className="fixed inset-0 bg-background/80 backdrop-blur-sm z-50 flex items-center justify-center p-4" onClick={() => setConfirmIgnore(null)}>
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-card border border-border rounded-xl p-6 max-w-md w-full shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <h2 className="font-display text-lg font-bold text-foreground mb-2">Ignore Subscription?</h2>
            <p className="text-sm text-muted-foreground mb-1">
              Are you sure you want to ignore <strong className="text-foreground">{confirmIgnore.merchant}</strong>?
            </p>
            <p className="text-xs text-muted-foreground mb-6">It will be hidden from your list and alerts.</p>
            <div className="flex gap-3 justify-end">
              <button onClick={() => setConfirmIgnore(null)} className="px-4 py-2 bg-muted text-muted-foreground rounded-lg text-sm font-medium hover:text-foreground transition-all">Cancel</button>
              <button onClick={confirmIgnoreAction} className="px-4 py-2 bg-destructive text-destructive-foreground rounded-lg text-sm font-semibold hover:opacity-90 transition-all">Ignore</button>
            </div>
          </motion.div>
        </div>
      )}

      {/* Add Subscription Modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-background/80 backdrop-blur-sm z-50 flex items-center justify-center p-4" onClick={() => setShowAddModal(false)}>
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-card border border-border rounded-xl p-6 max-w-md w-full shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <h2 className="font-display text-lg font-bold text-foreground mb-1">Add Subscription</h2>
            <p className="text-xs text-muted-foreground mb-5">Manually track a recurring payment</p>
            <form onSubmit={handleAddSubscription} className="space-y-3">
              <div className="flex flex-col gap-1">
                <label className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider">Subscription Name</label>
                <input
                  type="text"
                  required
                  value={addForm.merchant}
                  onChange={(e) => { setAddForm({ ...addForm, merchant: e.target.value }); setFormErrors({ ...formErrors, merchant: '' }); }}
                  placeholder="e.g. Netflix, Spotify"
                  className={`px-3 py-2.5 bg-input border rounded-lg text-foreground text-sm placeholder:text-muted-foreground focus:outline-none focus:border-primary transition-all ${
                    formErrors.merchant ? 'border-destructive' : 'border-border'
                  }`}
                />
                {formErrors.merchant && <p className="text-xs text-destructive mt-1">{formErrors.merchant}</p>}
              </div>
              <div className="flex flex-col gap-1">
                <label className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider">Monthly Amount ($)</label>
                <input
                  type="number"
                  required
                  step="0.01"
                  min="0"
                  max="10000"
                  value={addForm.avgAmount}
                  onChange={(e) => { setAddForm({ ...addForm, avgAmount: e.target.value }); setFormErrors({ ...formErrors, avgAmount: '' }); }}
                  placeholder="15.99"
                  className={`px-3 py-2.5 bg-input border rounded-lg text-foreground text-sm placeholder:text-muted-foreground focus:outline-none focus:border-primary transition-all ${
                    formErrors.avgAmount ? 'border-destructive' : 'border-border'
                  }`}
                />
                {formErrors.avgAmount && <p className="text-xs text-destructive mt-1">{formErrors.avgAmount}</p>}
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div className="flex flex-col gap-1">
                  <label className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider">Last Paid Date</label>
                  <input
                    type="date"
                    required
                    max={new Date().toISOString().split('T')[0]}
                    value={addForm.lastPaidDate}
                    onChange={(e) => { setAddForm({ ...addForm, lastPaidDate: e.target.value }); setFormErrors({ ...formErrors, lastPaidDate: '' }); }}
                    className={`px-3 py-2.5 bg-input border rounded-lg text-foreground text-sm focus:outline-none focus:border-primary transition-all ${
                      formErrors.lastPaidDate ? 'border-destructive' : 'border-border'
                    }`}
                  />
                  {formErrors.lastPaidDate && <p className="text-xs text-destructive mt-1">{formErrors.lastPaidDate}</p>}
                </div>
                <div className="flex flex-col gap-1">
                  <label className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider">Next Due Date</label>
                  <input
                    type="date"
                    required
                    min={new Date().toISOString().split('T')[0]}
                    value={addForm.nextDueDate}
                    onChange={(e) => { setAddForm({ ...addForm, nextDueDate: e.target.value }); setFormErrors({ ...formErrors, nextDueDate: '' }); }}
                    className={`px-3 py-2.5 bg-input border rounded-lg text-foreground text-sm focus:outline-none focus:border-primary transition-all ${
                      formErrors.nextDueDate ? 'border-destructive' : 'border-border'
                    }`}
                  />
                  {formErrors.nextDueDate && <p className="text-xs text-destructive mt-1">{formErrors.nextDueDate}</p>}
                </div>
              </div>
              <p className="text-xs text-muted-foreground mt-2">
                Note: Next due date must be 25-35 days after last paid date
              </p>
              <div className="flex gap-3 justify-end mt-6">
                <button 
                  type="button"
                  onClick={() => { setShowAddModal(false); setFormErrors({}); }} 
                  className="px-4 py-2 bg-muted text-muted-foreground rounded-lg text-sm font-medium hover:text-foreground transition-all"
                >
                  Cancel
                </button>
                <button 
                  type="submit"
                  disabled={submitting}
                  className="px-4 py-2 bg-primary text-primary-foreground rounded-lg text-sm font-semibold hover:bg-accent transition-all disabled:opacity-50 flex items-center gap-2"
                >
                  {submitting && <Loader2 className="w-4 h-4 animate-spin" />}
                  Add Subscription
                </button>
              </div>
            </form>
          </motion.div>
        </div>
      )}

      {/* Past Due Banner */}
      {pastDue.length > 0 && (
        <motion.div variants={item} className="mb-6 p-4 bg-destructive/10 border border-destructive/25 rounded-xl flex items-center gap-4">
          <Clock className="w-8 h-8 text-destructive shrink-0" />
          <div>
            <p className="text-sm font-bold text-destructive">Past Due Payments</p>
            <p className="text-xs text-destructive/80">
              You have {pastDue.length} overdue subscription{pastDue.length > 1 ? 's' : ''} that need attention
            </p>
          </div>
        </motion.div>
      )}

      {/* Due Soon Banner */}
      {dueSoon.length > 0 && (
        <motion.div variants={item} className="mb-6 p-4 bg-warning/10 border border-warning/25 rounded-xl flex items-center gap-4">
          <Clock className="w-8 h-8 text-warning shrink-0" />
          <div>
            <p className="text-sm font-bold text-warning">Upcoming Payments</p>
            <p className="text-xs text-warning/80">
              You have {dueSoon.length} subscription{dueSoon.length > 1 ? 's' : ''} due within 7 days
            </p>
          </div>
        </motion.div>
      )}

      {/* Subscription Cards */}
      <motion.div variants={item}>
        <Card className="border-border bg-card">
          <CardHeader className="pb-4">
            <CardTitle className="text-base font-display font-semibold">All Subscriptions ({activeSubs.length})</CardTitle>
          </CardHeader>
          <CardContent>
            {activeSubs.length === 0 ? (
              <div className="text-center py-12">
                <p className="text-sm text-muted-foreground mb-2">No active subscriptions detected</p>
                <p className="text-xs text-muted-foreground">
                  Create transactions with the same category 20-40 days apart to detect subscriptions
                </p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
                {activeSubs.map((sub) => {
                  const daysUntil = getDaysUntil(sub.nextDueDate);
                  const isPastDue = daysUntil < 0;
                  const isDueSoon = daysUntil >= 0 && daysUntil <= 7;
                  return (
                    <div
                      key={sub.id}
                      className={`p-4 rounded-xl border transition-all hover:-translate-y-0.5 hover:shadow-lg flex flex-col gap-3 ${
                        isPastDue
                          ? 'border-destructive/30 bg-destructive/5'
                          : isDueSoon
                          ? 'border-warning/30 bg-warning/5'
                          : 'border-border bg-card hover:border-primary/30'
                      }`}
                    >
                      <div className="flex items-start justify-between">
                        <div className="w-10 h-10 rounded-lg bg-info/10 flex items-center justify-center text-base">📱</div>
                        {isPastDue
                          ? <Badge variant="outline" className="text-[10px] h-5 bg-destructive/10 text-destructive border-destructive/20">Past Due</Badge>
                          : isDueSoon
                          ? <Badge variant="outline" className="text-[10px] h-5 bg-warning/10 text-warning border-warning/20">Due Soon</Badge>
                          : <Badge variant="outline" className="text-[10px] h-5 bg-success/10 text-success border-success/20">Active</Badge>}
                      </div>

                      <p className="font-display text-base font-bold text-foreground">{sub.merchant}</p>

                      <div className="flex items-baseline gap-1">
                        <span className="font-display text-xl font-extrabold text-primary">{formatCurrency(sub.avgAmount)}</span>
                        <span className="text-xs text-muted-foreground">/month</span>
                      </div>

                      <div className="bg-muted/50 rounded-lg p-3 space-y-2 text-xs">
                        <div className="flex justify-between">
                          <span className="text-muted-foreground">Last Paid</span>
                          <span className="font-medium text-foreground/80">{formatDate(sub.lastPaidDate)}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-muted-foreground">Next Due</span>
                          <span className={`font-medium ${isPastDue ? 'text-destructive' : isDueSoon ? 'text-warning' : 'text-foreground/80'}`}>
                            {formatDate(sub.nextDueDate)}
                            {isPastDue && ` · ${Math.abs(daysUntil)}d overdue`}
                            {isDueSoon && ` · ${daysUntil}d`}
                          </span>
                        </div>
                      </div>

                      <button
                        onClick={() => handleIgnore(sub)}
                        className="mt-auto px-3 py-1.5 bg-muted text-muted-foreground rounded-lg text-xs font-medium hover:text-foreground transition-all self-start"
                      >
                        Ignore
                      </button>
                    </div>
                  );
                })}
              </div>
            )}
          </CardContent>
        </Card>
      </motion.div>
    </motion.div>
  );
};

export default Subscriptions;
