import React from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Navbar.css';

const Navbar = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const isActive = (path) => location.pathname === path;

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="navbar">
      <div className="navbar-container">
        <Link to="/" className="navbar-logo">
          <span className="logo-icon">💰</span>
          <span className="logo-text">FinSight</span>
        </Link>
        <ul className="navbar-menu">
          <li>
            <Link 
              to="/dashboard" 
              className={`navbar-link ${isActive('/dashboard') ? 'active' : ''}`}
            >
              Dashboard
            </Link>
          </li>
          <li>
            <Link 
              to="/transactions" 
              className={`navbar-link ${isActive('/transactions') ? 'active' : ''}`}
            >
              Transactions
            </Link>
          </li>
          <li>
            <Link 
              to="/fraud-alerts" 
              className={`navbar-link ${isActive('/fraud-alerts') ? 'active' : ''}`}
            >
              Fraud Alerts
            </Link>
          </li>
          <li>
            <Link 
              to="/subscriptions" 
              className={`navbar-link ${isActive('/subscriptions') ? 'active' : ''}`}
            >
              Subscriptions
            </Link>
          </li>
          <li className="navbar-user">
            <span className="user-name">{user?.username}</span>
            <button onClick={handleLogout} className="logout-button">
              Logout
            </button>
          </li>
        </ul>
      </div>
    </nav>
  );
};

export default Navbar;
