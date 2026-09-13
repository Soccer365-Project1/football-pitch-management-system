import React, { useState, useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import CustomerHeader from '../components/layout/CustomerHeader';
import CustomerFooter from '../components/layout/CustomerFooter';

const CustomerLayout: React.FC = () => {
  const [theme, setTheme] = useState<'light' | 'dark'>('light');

  useEffect(() => {
    // Kiểm tra theme lưu trong localStorage hoặc theo hệ điều hành
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

  return (
    <div className="customer-layout">
      <CustomerHeader
        theme={theme}
        toggleTheme={toggleTheme}
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
