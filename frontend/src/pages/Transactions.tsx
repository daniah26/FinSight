import { useState, useMemo, useEffect } from 'react';
import { motion } from 'framer-motion';
import { getAllTransactions, createTransaction } from '../lib/api';
import { useAuth } from '../context/AuthContext';
import type { Transaction } from '../lib/mockData';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Plus, X, Search, ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight, Loader2 } from 'lucide-react';

const PAGE_SIZE = 20;

const formatCurrency = (amount: number) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);

const formatDate = (dateString: string) =>
  new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  });

const container = { hidden: {}, show: { transition: { staggerChildren: 0.04 } } };
const item = { hidden: { opacity: 0, y: 8 }, show: { opacity: 1, y: 0 } };

const Transactions = () => {
  const { user } = useAuth();
  const [allTransactions, setAllTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [typeFilter, setTypeFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [fraudulentFilter, setFraudulentFilter] = useState('');
  const [dateFilter, setDateFilter] = useState('all');
  const [customStartDate, setCustomStartDate] = useState('');
  const [customEndDate, setCustomEndDate] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [sortBy, setSortBy] = useState<'transactionDate' | 'amount' | 'category'>('transactionDate');
  const [sortDir, setSortDir] = useState<'ASC' | 'DESC'>('DESC');
  const [currentPage, setCurrentPage] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});

  const [formData, setFormData] = useState({
    amount: '',
    type: 'EXPENSE',
    category: '',
    description: '',
    location: '',
    transactionDate: new Date().toISOString().slice(0, 16),
  });

  useEffect(() => {
    if (!user?.id) return;
    const fetchTransactions = async () => {
      try {
        setLoading(true);
        const res = await getAllTransactions(user.id);
        const data = res.data?.content || res.data || [];
        setAllTransactions(Array.isArray(data) ? data : []);
      } catch (err: any) {
        setError(err.response?.data?.message || 'Failed to load transactions');
      } finally {
        setLoading(false);
      }
    };
    fetchTransactions();
  }, [user?.id]);

  const handleCreateTransaction = async () => {
    if (!user?.id || !formData.amount || !formData.category) return;
    
    // Clear previous errors
    setFormErrors({});
    setError('');
    
    // Client-side validation
    const errors: Record<string, string> = {};
    const amount = parseFloat(formData.amount);
    const txDate = new Date(formData.transactionDate);
    const now = new Date();
    
    if (isNaN(amount) || amount <= 0) {
      errors.amount = 'Amount must be greater than $0';
    } else if (amount > 1000000) {
      errors.amount = 'Amount cannot exceed $1,000,000';
    }
    
    if (txDate > now) {
      errors.transactionDate = 'Transaction date cannot be in the future';
    }
    
    if (!formData.category.trim()) {
      errors.category = 'Category is required';
    } else if (formData.category.length > 50) {
      errors.category = 'Category must not exceed 50 characters';
    }
    
    if (formData.description && formData.description.length > 255) {
      errors.description = 'Description must not exceed 255 characters';
    }
    
    if (formData.location && formData.location.length > 100) {
      errors.location = 'Location must not exceed 100 characters';
    }
    
    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }
    
    try {
      setSubmitting(true);
      await createTransaction({
        amount: amount,
        type: formData.type,
        category: formData.category.trim(),
        description: formData.description,
        location: formData.location,
        transactionDate: formData.transactionDate,
        userId: user.id,
      });
      // Refresh
      const res = await getAllTransactions(user.id);
      const data = res.data?.content || res.data || [];
      setAllTransactions(Array.isArray(data) ? data : []);
      setShowForm(false);
      setFormData({ amount: '', type: 'EXPENSE', category: '', description: '', location: '', transactionDate: new Date().toISOString().slice(0, 16) });
      setFormErrors({});
    } catch (err: any) {
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
        setError(err.response?.data?.message || 'Failed to create transaction');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const getDateRange = () => {
    const now = new Date();
    switch (dateFilter) {
      case 'last7': return { start: new Date(now.getTime() - 7 * 86400000), end: now };
      case 'last30': return { start: new Date(now.getTime() - 30 * 86400000), end: now };
      case 'thisMonth': return { start: new Date(now.getFullYear(), now.getMonth(), 1), end: now };
      case 'lastMonth': return { start: new Date(now.getFullYear(), now.getMonth() - 1, 1), end: new Date(now.getFullYear(), now.getMonth(), 0) };
      case 'custom': return {
        start: customStartDate ? new Date(customStartDate) : null,
        end: customEndDate ? new Date(customEndDate) : null,
      };
      default: return { start: null, end: null };
    }
  };

  const filtered = useMemo(() => {
    let result = [...allTransactions];

    if (typeFilter) result = result.filter(t => t.type === typeFilter);
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      result = result.filter(t =>
        t.category?.toLowerCase().includes(q) ||
        t.description?.toLowerCase().includes(q) ||
        t.location?.toLowerCase().includes(q)
      );
    }
    if (categoryFilter) {
      const c = categoryFilter.toLowerCase();
      result = result.filter(t => t.category?.toLowerCase().includes(c));
    }
    if (fraudulentFilter !== '') {
      const isFraud = fraudulentFilter === 'true';
      result = result.filter(t => t.fraudulent === isFraud);
    }

    const { start, end } = getDateRange();
    if (start) result = result.filter(t => new Date(t.transactionDate) >= start);
    if (end) result = result.filter(t => new Date(t.transactionDate) <= end);

    result.sort((a, b) => {
      let valA: number | string, valB: number | string;
      if (sortBy === 'amount') { valA = a.amount; valB = b.amount; }
      else if (sortBy === 'category') { valA = a.category; valB = b.category; }
      else { valA = new Date(a.transactionDate).getTime(); valB = new Date(b.transactionDate).getTime(); }

      if (typeof valA === 'string' && typeof valB === 'string') {
        return sortDir === 'DESC' ? valB.localeCompare(valA) : valA.localeCompare(valB);
      }
      return sortDir === 'DESC' ? (valB as number) - (valA as number) : (valA as number) - (valB as number);
    });
    return result;
  }, [allTransactions, typeFilter, searchQuery, categoryFilter, fraudulentFilter, dateFilter, customStartDate, customEndDate, sortBy, sortDir]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const paginated = filtered.slice(currentPage * PAGE_SIZE, (currentPage + 1) * PAGE_SIZE);

  useMemo(() => setCurrentPage(0), [typeFilter, searchQuery, categoryFilter, fraudulentFilter, dateFilter, customStartDate, customEndDate]);

  const handleSort = (field: typeof sortBy) => {
    if (sortBy === field) setSortDir(d => d === 'ASC' ? 'DESC' : 'ASC');
    else { setSortBy(field); setSortDir('DESC'); }
  };

  const clearFilters = () => {
    setTypeFilter(''); setSearchQuery(''); setCategoryFilter('');
    setFraudulentFilter(''); setDateFilter('all');
    setCustomStartDate(''); setCustomEndDate('');
  };

  const hasActiveFilters = typeFilter || searchQuery || categoryFilter || fraudulentFilter || dateFilter !== 'all';

  const selectClasses = "px-3 py-2 bg-input border border-border rounded-lg text-foreground text-sm focus:outline-none focus:border-primary transition-all appearance-none cursor-pointer";
  const inputClasses = "px-3 py-2.5 bg-input border border-border rounded-lg text-foreground text-sm placeholder:text-muted-foreground focus:outline-none focus:border-primary transition-all";

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
          <h1 className="font-display text-2xl font-bold text-foreground tracking-tight">Transactions</h1>
          <p className="text-sm text-muted-foreground mt-1">{filtered.length} transactions found</p>
        </div>
        <button
          onClick={() => setShowForm(!showForm)}
          className="flex items-center gap-2 px-4 py-2.5 bg-primary text-primary-foreground rounded-lg text-sm font-semibold hover:bg-accent transition-all"
        >
          {showForm ? <X className="w-4 h-4" /> : <Plus className="w-4 h-4" />}
          {showForm ? 'Cancel' : 'Add Transaction'}
        </button>
      </motion.div>

      {error && (
        <div className="mb-4 p-3 bg-destructive/10 text-destructive rounded-lg text-sm">{error}</div>
      )}

      {showForm && (
        <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: 'auto' }} exit={{ opacity: 0 }}>
          <Card className="border-border bg-card mb-6">
            <CardHeader><CardTitle className="text-base font-display">New Transaction</CardTitle></CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="flex flex-col gap-1.5">
                  <label className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Amount *</label>
                  <input type="number" step="0.01" required value={formData.amount}
                    onChange={(e) => { setFormData({ ...formData, amount: e.target.value }); setFormErrors({ ...formErrors, amount: '' }); }}
                    placeholder="0.00" 
                    className={`${inputClasses} ${formErrors.amount ? 'border-destructive' : ''}`} />
                  {formErrors.amount && <p className="text-xs text-destructive mt-1">{formErrors.amount}</p>}
                </div>
                <div className="flex flex-col gap-1.5">
                  <label className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Type *</label>
                  <select value={formData.type} onChange={(e) => setFormData({ ...formData, type: e.target.value })} className={selectClasses}>
                    <option value="EXPENSE">Expense</option>
                    <option value="INCOME">Income</option>
                  </select>
                </div>
                <div className="flex flex-col gap-1.5">
                  <label className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Category *</label>
                  <input type="text" required value={formData.category}
                    onChange={(e) => { setFormData({ ...formData, category: e.target.value }); setFormErrors({ ...formErrors, category: '' }); }}
                    placeholder="e.g., groceries, salary" 
                    className={`${inputClasses} ${formErrors.category ? 'border-destructive' : ''}`} />
                  {formErrors.category && <p className="text-xs text-destructive mt-1">{formErrors.category}</p>}
                </div>
                <div className="flex flex-col gap-1.5">
                  <label className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Date *</label>
                  <input type="datetime-local" required value={formData.transactionDate}
                    onChange={(e) => { setFormData({ ...formData, transactionDate: e.target.value }); setFormErrors({ ...formErrors, transactionDate: '' }); }}
                    className={`${inputClasses} ${formErrors.transactionDate ? 'border-destructive' : ''}`} />
                  {formErrors.transactionDate && <p className="text-xs text-destructive mt-1">{formErrors.transactionDate}</p>}
                </div>
                <div className="flex flex-col gap-1.5">
                  <label className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Description</label>
                  <input type="text" value={formData.description}
                    onChange={(e) => { setFormData({ ...formData, description: e.target.value }); setFormErrors({ ...formErrors, description: '' }); }}
                    placeholder="Optional description" 
                    className={`${inputClasses} ${formErrors.description ? 'border-destructive' : ''}`} />
                  {formErrors.description && <p className="text-xs text-destructive mt-1">{formErrors.description}</p>}
                </div>
                <div className="flex flex-col gap-1.5">
                  <label className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Location</label>
                  <input type="text" value={formData.location}
                    onChange={(e) => { setFormData({ ...formData, location: e.target.value }); setFormErrors({ ...formErrors, location: '' }); }}
                    placeholder="Optional location" 
                    className={`${inputClasses} ${formErrors.location ? 'border-destructive' : ''}`} />
                  {formErrors.location && <p className="text-xs text-destructive mt-1">{formErrors.location}</p>}
                </div>
              </div>
              <div className="flex gap-3 mt-4">
                <button
                  onClick={handleCreateTransaction}
                  disabled={submitting}
                  className="px-4 py-2 bg-primary text-primary-foreground rounded-lg text-sm font-semibold hover:bg-accent transition-all disabled:opacity-50"
                >
                  {submitting ? 'Creating...' : 'Create Transaction'}
                </button>
                <button onClick={() => setShowForm(false)}
                  className="px-4 py-2 bg-muted text-muted-foreground rounded-lg text-sm font-medium hover:text-foreground transition-all">
                  Cancel
                </button>
              </div>
            </CardContent>
          </Card>
        </motion.div>
      )}

      {/* Filters */}
      <motion.div variants={item}>
        <Card className="border-border bg-card mb-6">
          <CardContent className="p-4 space-y-4">
            <div className="flex flex-wrap items-center gap-3">
              <div className="relative flex-1 min-w-[200px]">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                <input type="text" value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Search transactions..."
                  className="w-full pl-9 pr-3 py-2 bg-input border border-border rounded-lg text-foreground text-sm placeholder:text-muted-foreground focus:outline-none focus:border-primary transition-all" />
              </div>
              <select value={dateFilter} onChange={(e) => setDateFilter(e.target.value)} className={selectClasses}>
                <option value="all">All Time</option>
                <option value="last7">Last 7 Days</option>
                <option value="last30">Last 30 Days</option>
                <option value="thisMonth">This Month</option>
                <option value="lastMonth">Last Month</option>
                <option value="custom">Custom Range</option>
              </select>
              <select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)} className={selectClasses}>
                <option value="">All Types</option>
                <option value="INCOME">Income</option>
                <option value="EXPENSE">Expense</option>
              </select>
              <select value={fraudulentFilter} onChange={(e) => setFraudulentFilter(e.target.value)} className={selectClasses}>
                <option value="">All Status</option>
                <option value="true">Fraudulent Only</option>
                <option value="false">Non-Fraudulent</option>
              </select>
            </div>

            {dateFilter === 'custom' && (
              <div className="flex flex-wrap items-center gap-3">
                <div className="flex flex-col gap-1">
                  <label className="text-[10px] font-semibold text-muted-foreground uppercase">Start Date</label>
                  <input type="date" value={customStartDate} onChange={(e) => setCustomStartDate(e.target.value)} className={inputClasses} />
                </div>
                <div className="flex flex-col gap-1">
                  <label className="text-[10px] font-semibold text-muted-foreground uppercase">End Date</label>
                  <input type="date" value={customEndDate} onChange={(e) => setCustomEndDate(e.target.value)} className={inputClasses} />
                </div>
              </div>
            )}

            <div className="flex flex-wrap items-center gap-3">
              <input type="text" value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value)}
                placeholder="Filter by category..."
                className="px-3 py-2 bg-input border border-border rounded-lg text-foreground text-sm placeholder:text-muted-foreground focus:outline-none focus:border-primary transition-all min-w-[160px]" />

              <div className="flex items-center gap-1 text-xs text-muted-foreground ml-auto">
                <span>Sort:</span>
                {(['transactionDate', 'amount', 'category'] as const).map((field) => (
                  <button key={field} onClick={() => handleSort(field)}
                    className={`px-2 py-1 rounded text-xs font-medium transition-all capitalize ${
                      sortBy === field ? 'bg-primary/20 text-primary' : 'hover:text-foreground'
                    }`}>
                    {field === 'transactionDate' ? 'Date' : field}
                    {sortBy === field && (sortDir === 'ASC' ? ' ↑' : ' ↓')}
                  </button>
                ))}
              </div>

              {hasActiveFilters && (
                <button onClick={clearFilters}
                  className="px-3 py-1.5 bg-muted text-muted-foreground rounded-lg text-xs font-medium hover:text-foreground transition-all">
                  Clear All
                </button>
              )}
            </div>
          </CardContent>
        </Card>
      </motion.div>

      {/* Transaction List */}
      <motion.div variants={item}>
        <Card className="border-border bg-card">
          <CardHeader className="pb-3">
            <CardTitle className="text-base font-display font-semibold">
              Transactions ({filtered.length} total, page {currentPage + 1} of {totalPages})
            </CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            <div className="divide-y divide-border">
              {paginated.map((tx) => (
                <div key={tx.id}
                  className={`flex items-center justify-between p-4 hover:bg-muted/30 transition-colors ${
                    tx.fraudulent ? 'bg-danger/5' : ''
                  }`}>
                  <div className="flex items-center gap-3">
                    <div className={`w-10 h-10 rounded-lg flex items-center justify-center text-base ${
                      tx.type === 'INCOME' ? 'bg-success/10 text-success' : 'bg-danger/10 text-danger'
                    }`}>
                      {tx.type === 'INCOME' ? '↑' : '↓'}
                    </div>
                    <div>
                      <p className="text-sm font-medium text-foreground capitalize">{tx.category}</p>
                      <p className="text-xs text-muted-foreground">{tx.description}</p>
                      <p className="text-[11px] text-muted-foreground/60 mt-0.5">{formatDate(tx.transactionDate)}</p>
                    </div>
                  </div>
                  <div className="text-right flex flex-col items-end gap-1">
                    <p className={`text-sm font-bold font-display ${
                      tx.type === 'INCOME' ? 'text-success' : 'text-foreground'
                    }`}>
                      {tx.type === 'INCOME' ? '+' : '-'}{formatCurrency(tx.amount)}
                    </p>
                    <div className="flex gap-1.5">
                      {tx.fraudulent && (
                        <>
                          <Badge variant="destructive" className="text-[10px] h-5 px-1.5">{tx.riskLevel}</Badge>
                          <Badge variant="destructive" className="text-[10px] h-5 px-1.5">Score: {tx.fraudScore}</Badge>
                        </>
                      )}
                      <Badge variant="secondary"
                        className={`text-[10px] h-5 px-1.5 ${
                          tx.type === 'INCOME' ? 'bg-success/10 text-success border-success/20' : 'bg-info/10 text-info border-info/20'
                        }`}>
                        {tx.type}
                      </Badge>
                    </div>
                  </div>
                </div>
              ))}
              {paginated.length === 0 && (
                <div className="text-center py-12 text-muted-foreground text-sm">No transactions found</div>
              )}
            </div>

            {totalPages > 1 && (
              <div className="flex items-center justify-center gap-1 p-4 border-t border-border">
                <button onClick={() => setCurrentPage(0)} disabled={currentPage === 0}
                  className="p-1.5 rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted disabled:opacity-30 disabled:pointer-events-none transition-all">
                  <ChevronsLeft className="w-4 h-4" />
                </button>
                <button onClick={() => setCurrentPage(p => p - 1)} disabled={currentPage === 0}
                  className="p-1.5 rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted disabled:opacity-30 disabled:pointer-events-none transition-all">
                  <ChevronLeft className="w-4 h-4" />
                </button>

                {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                  let start = Math.max(0, currentPage - 2);
                  if (start + 5 > totalPages) start = Math.max(0, totalPages - 5);
                  const page = start + i;
                  if (page >= totalPages) return null;
                  return (
                    <button key={page} onClick={() => setCurrentPage(page)}
                      className={`w-8 h-8 rounded-lg text-xs font-medium transition-all ${
                        currentPage === page
                          ? 'bg-primary text-primary-foreground'
                          : 'text-muted-foreground hover:text-foreground hover:bg-muted'
                      }`}>
                      {page + 1}
                    </button>
                  );
                })}

                <button onClick={() => setCurrentPage(p => p + 1)} disabled={currentPage >= totalPages - 1}
                  className="p-1.5 rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted disabled:opacity-30 disabled:pointer-events-none transition-all">
                  <ChevronRight className="w-4 h-4" />
                </button>
                <button onClick={() => setCurrentPage(totalPages - 1)} disabled={currentPage >= totalPages - 1}
                  className="p-1.5 rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted disabled:opacity-30 disabled:pointer-events-none transition-all">
                  <ChevronsRight className="w-4 h-4" />
                </button>
              </div>
            )}
          </CardContent>
        </Card>
      </motion.div>
    </motion.div>
  );
};

export default Transactions;
