import React, { useState, useEffect } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import CustomerHeader from '../components/layout/CustomerHeader';
import CustomerFooter from '../components/layout/CustomerFooter';

const CustomerLayout: React.FC = () => {
  const [theme, setTheme] = useState<'light' | 'dark'>('light');
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();

  const isProfilePage = location.pathname === '/profile';
  const isAuthRequiredPage = location.pathname.startsWith('/checkout') || location.pathname === '/my-bookings' || location.pathname === '/profile';

  const [isLoggedIn, setIsLoggedIn] = useState<boolean>(() => {
    return localStorage.getItem('isLoggedIn') === 'true' || isAuthRequiredPage;
  });

  const showUserMenu = isLoggedIn || isAuthRequiredPage;

  useEffect(() => {
    // Close mobile menu when route changes
    setIsMobileMenuOpen(false);
  }, [location]);

  useEffect(() => {
    if (isAuthRequiredPage) {
      setIsLoggedIn(true);
      localStorage.setItem('isLoggedIn', 'true');
    }
  }, [location.pathname, isAuthRequiredPage]);

  useEffect(() => {
    // Check local storage or system preference on mount
    const savedTheme = localStorage.getItem('theme') as 'light' | 'dark' | null;
    if (savedTheme) {
      setTheme(savedTheme);
      document.documentElement.setAttribute('data-theme', savedTheme);
    } else {
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      setTheme(prefersDark ? 'dark' : 'light');
      document.documentElement.setAttribute('data-theme', prefersDark ? 'dark' : 'light');
    }
  }, []);

  const toggleTheme = () => {
    const newTheme = theme === 'light' ? 'dark' : 'light';
    setTheme(newTheme);
    document.documentElement.setAttribute('data-theme', newTheme);
    localStorage.setItem('theme', newTheme);
  };

  const handleLogout = () => {
    setIsLoggedIn(false);
    localStorage.removeItem('isLoggedIn');
    navigate('/login');
  };

  return (
    <div className="customer-layout">
      <CustomerHeader
        theme={theme}
        toggleTheme={toggleTheme}
        isLoggedIn={isLoggedIn}
        handleLogout={handleLogout}
        showUserMenu={showUserMenu}
        isProfilePage={isProfilePage}
      />

      <main className="customer-main">
        <div className="animate-fade-in">
          <Outlet />
        </div>
      </main>

      <CustomerFooter />
    </div>
  );
};

export default CustomerLayout;
