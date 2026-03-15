import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { DollarSign, Eye, EyeOff, Sun, Moon } from 'lucide-react';
import { motion } from 'framer-motion';
import { registerUser } from '../lib/api';

const Signup = () => {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [touched, setTouched] = useState({
    username: false,
    email: false,
    password: false,
    confirmPassword: false,
  });
  const [validationErrors, setValidationErrors] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
  });
  
  const navigate = useNavigate();
  const { login } = useAuth();
  const { theme, toggleTheme } = useTheme();

  // Validation functions
  const validateUsername = (username: string) => {
    if (username.length === 0) return '';
    if (username.length < 3) return 'Username must be at least 3 characters';
    if (username.length > 20) return 'Username must not exceed 20 characters';
    if (!/^[a-zA-Z0-9_]+$/.test(username)) return 'Username can only contain letters, numbers, and underscores';
    return '';
  };

  const validateEmail = (email: string) => {
    if (email.length === 0) return '';
    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    if (!emailRegex.test(email)) return 'Please enter a valid email address (e.g., user@example.com)';
    return '';
  };

  const validatePassword = (password: string) => {
    if (password.length === 0) return '';
    if (password.length < 8) return 'Password must be at least 8 characters';
    if (password.length > 128) return 'Password must not exceed 128 characters';
    if (!/(?=.*[a-z])/.test(password)) return 'Password must contain at least one lowercase letter';
    if (!/(?=.*[A-Z])/.test(password)) return 'Password must contain at least one uppercase letter';
    if (!/(?=.*\d)/.test(password)) return 'Password must contain at least one digit';
    if (!/(?=.*[@$!%*?&])/.test(password)) return 'Password must contain at least one special character (@$!%*?&)';
    return '';
  };

  const validateConfirmPassword = (confirmPassword: string, password: string) => {
    if (confirmPassword.length === 0) return '';
    if (confirmPassword !== password) return 'Passwords do not match';
    return '';
  };

  // Real-time validation
  useEffect(() => {
    setValidationErrors({
      username: touched.username ? validateUsername(formData.username) : '',
      email: touched.email ? validateEmail(formData.email) : '',
      password: touched.password ? validatePassword(formData.password) : '',
      confirmPassword: touched.confirmPassword ? validateConfirmPassword(formData.confirmPassword, formData.password) : '',
    });
  }, [formData, touched]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setError('');
  };

  const handleBlur = (field: keyof typeof touched) => {
    setTouched({ ...touched, [field]: true });
  };

  const validateForm = () => {
    const usernameError = validateUsername(formData.username);
    const emailError = validateEmail(formData.email);
    const passwordError = validatePassword(formData.password);
    const confirmPasswordError = validateConfirmPassword(formData.confirmPassword, formData.password);

    if (usernameError) {
      setError(usernameError);
      return false;
    }
    if (emailError) {
      setError(emailError);
      return false;
    }
    if (passwordError) {
      setError(passwordError);
      return false;
    }
    if (confirmPasswordError) {
      setError(confirmPasswordError);
      return false;
    }
    return true;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    if (!validateForm()) return;
    setLoading(true);

    try {
      const response = await registerUser({
        username: formData.username,
        email: formData.email,
        password: formData.password
      });
      
      login({
        token: response.data.token,
        id: response.data.userId,
        username: response.data.username,
        role: 'USER' // Default role since backend doesn't return it
      });
      
      navigate('/dashboard');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Registration failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen grid grid-cols-1 lg:grid-cols-2 bg-background relative">
      <button
        onClick={toggleTheme}
        className="absolute top-5 right-5 z-50 w-9 h-9 rounded-lg bg-card border border-border flex items-center justify-center text-muted-foreground hover:text-foreground hover:border-primary/40 transition-all"
        aria-label="Toggle theme"
      >
        {theme === 'dark' ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
      </button>
      {/* Left decorative panel */}
      <div className="hidden lg:flex flex-col justify-between p-10 bg-card border-r border-border relative overflow-hidden">
        <div className="absolute -top-[30%] -left-[20%] w-[60%] h-[60%] rounded-full bg-primary/10 blur-3xl pointer-events-none" />
        <div className="absolute -bottom-[20%] -right-[20%] w-[50%] h-[50%] rounded-full bg-secondary/15 blur-3xl pointer-events-none" />

        <div className="flex items-center gap-3 relative z-10">
          <div className="w-10 h-10 rounded-lg bg-primary flex items-center justify-center">
            <DollarSign className="w-5 h-5 text-primary-foreground" />
          </div>
          <span className="font-display text-xl font-bold text-foreground">
            Fin<span className="text-primary">Sight</span>
          </span>
        </div>

        <motion.div
          className="relative z-10"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.2 }}
        >
          <h2 className="font-display text-4xl font-extrabold text-foreground leading-tight mb-4 tracking-tight">
            Start tracking<br /><span className="text-primary">smarter today.</span>
          </h2>
          <p className="text-muted-foreground text-base leading-relaxed max-w-sm">
            Join users who trust FinSight to keep their finances transparent and secure.
          </p>
        </motion.div>

        <div className="relative z-10">
          {/* Empty space where stats used to be */}
        </div>
      </div>

      {/* Right form panel */}
      <div className="flex items-center justify-center p-6 lg:p-10">
        <motion.div
          className="w-full max-w-md"
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4 }}
        >
          <div className="lg:hidden flex items-center gap-3 mb-8">
            <div className="w-10 h-10 rounded-lg bg-primary flex items-center justify-center">
              <DollarSign className="w-5 h-5 text-primary-foreground" />
            </div>
            <span className="font-display text-xl font-bold text-foreground">
              Fin<span className="text-primary">Sight</span>
            </span>
          </div>

          <h2 className="font-display text-2xl font-bold text-foreground mb-1">Create Account</h2>
          <p className="text-muted-foreground text-sm mb-8">Sign up to get started</p>

          {error && (
            <div className="bg-destructive/10 border border-destructive/30 text-destructive px-4 py-3 rounded-lg mb-6 text-sm">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Username</label>
              <input
                type="text"
                name="username"
                value={formData.username}
                onChange={handleChange}
                onBlur={() => handleBlur('username')}
                required
                autoFocus
                placeholder="Choose a username"
                className={`px-4 py-3 bg-input border rounded-lg text-foreground text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-2 transition-all ${
                  validationErrors.username 
                    ? 'border-destructive focus:border-destructive focus:ring-destructive/20' 
                    : touched.username && formData.username && !validationErrors.username
                    ? 'border-green-500 focus:border-green-500 focus:ring-green-500/20'
                    : 'border-border focus:border-primary focus:ring-primary/20'
                }`}
              />
              {validationErrors.username && (
                <p className="text-xs text-destructive">{validationErrors.username}</p>
              )}
              {!validationErrors.username && touched.username && formData.username && (
                <p className="text-xs text-green-600">✓ Username is valid</p>
              )}
            </div>

            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Email</label>
              <input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                onBlur={() => handleBlur('email')}
                required
                placeholder="Enter your email"
                className={`px-4 py-3 bg-input border rounded-lg text-foreground text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-2 transition-all ${
                  validationErrors.email 
                    ? 'border-destructive focus:border-destructive focus:ring-destructive/20' 
                    : touched.email && formData.email && !validationErrors.email
                    ? 'border-green-500 focus:border-green-500 focus:ring-green-500/20'
                    : 'border-border focus:border-primary focus:ring-primary/20'
                }`}
              />
              {validationErrors.email && (
                <p className="text-xs text-destructive">{validationErrors.email}</p>
              )}
              {!validationErrors.email && touched.email && formData.email && (
                <p className="text-xs text-green-600">✓ Email is valid</p>
              )}
            </div>

            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Password</label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  onBlur={() => handleBlur('password')}
                  required
                  placeholder="Create a password"
                  className={`w-full px-4 py-3 bg-input border rounded-lg text-foreground text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-2 transition-all pr-10 ${
                    validationErrors.password 
                      ? 'border-destructive focus:border-destructive focus:ring-destructive/20' 
                      : touched.password && formData.password && !validationErrors.password
                      ? 'border-green-500 focus:border-green-500 focus:ring-green-500/20'
                      : 'border-border focus:border-primary focus:ring-primary/20'
                  }`}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {validationErrors.password && (
                <p className="text-xs text-destructive">{validationErrors.password}</p>
              )}
              {!validationErrors.password && touched.password && formData.password && (
                <p className="text-xs text-green-600">✓ Password meets all requirements</p>
              )}
            </div>

            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Confirm Password</label>
              <input
                type="password"
                name="confirmPassword"
                value={formData.confirmPassword}
                onChange={handleChange}
                onBlur={() => handleBlur('confirmPassword')}
                required
                placeholder="Confirm your password"
                className={`px-4 py-3 bg-input border rounded-lg text-foreground text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-2 transition-all ${
                  validationErrors.confirmPassword 
                    ? 'border-destructive focus:border-destructive focus:ring-destructive/20' 
                    : touched.confirmPassword && formData.confirmPassword && !validationErrors.confirmPassword
                    ? 'border-green-500 focus:border-green-500 focus:ring-green-500/20'
                    : 'border-border focus:border-primary focus:ring-primary/20'
                }`}
              />
              {validationErrors.confirmPassword && (
                <p className="text-xs text-destructive">{validationErrors.confirmPassword}</p>
              )}
              {!validationErrors.confirmPassword && touched.confirmPassword && formData.confirmPassword && (
                <p className="text-xs text-green-600">✓ Passwords match</p>
              )}
            </div>

            <button
              type="submit"
              disabled={loading || Object.values(validationErrors).some(error => error !== '')}
              className="w-full py-3 bg-primary text-primary-foreground rounded-lg text-sm font-semibold hover:bg-accent transition-all disabled:opacity-50 disabled:cursor-not-allowed mt-1 hover:-translate-y-0.5 active:translate-y-0"
            >
              {loading ? 'Creating account…' : 'Sign Up'}
            </button>
          </form>

          <div className="text-center mt-6 pt-6 border-t border-border">
            <p className="text-sm text-muted-foreground">
              Already have an account?{' '}
              <Link to="/login" className="text-primary font-semibold hover:underline">
                Sign in
              </Link>
            </p>
          </div>
        </motion.div>
      </div>
    </div>
  );
};

export default Signup;
