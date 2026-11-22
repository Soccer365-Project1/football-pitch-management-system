import React, { useState, useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import AdminSidebar from '../components/layout/AdminSidebar';
import AdminTopbar from '../components/layout/AdminTopbar';

const AdminLayout: React.FC = () => {
  const [theme, setTheme] = useState<'light' | 'dark'>('light');
  const [sidebarOpen, setSidebarOpen] = useState<boolean>(() => {
    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem('admin_sidebar_open');
      if (saved !== null) {
        return saved === 'true';
      }
      return window.innerWidth > 1024;
    }
    return true;
  });

  useEffect(() => {
    const savedTheme = localStorage.getItem('theme') as 'light' | 'dark' | null;
    if (savedTheme) {
      setTheme(savedTheme);
      document.documentElement.setAttribute('data-theme', savedTheme);
    }
  }, []);

  const toggleTheme = () => {
    const newTheme = theme === 'light' ? 'dark' : 'light';
    setTheme(newTheme);
    document.documentElement.setAttribute('data-theme', newTheme);
    localStorage.setItem('theme', newTheme);
  };

  const handleToggleSidebar = (openOrUpdater: boolean | ((prev: boolean) => boolean)) => {
    setSidebarOpen(prev => {
      const next = typeof openOrUpdater === 'function' ? openOrUpdater(prev) : openOrUpdater;
      if (typeof window !== 'undefined' && window.innerWidth > 1024) {
        localStorage.setItem('admin_sidebar_open', String(next));
      }
      return next;
    });
  };

  return (
    <div style={{ display: 'flex', height: '100vh', overflow: 'hidden', backgroundColor: 'var(--color-bg-base)' }}>
      <AdminSidebar sidebarOpen={sidebarOpen} setSidebarOpen={handleToggleSidebar} />

      {/* Main Content */}
      <div style={{ flexGrow: 1, display: 'flex', flexDirection: 'column', minWidth: 0, height: '100vh', transition: 'all 0.3s ease' }}>
        <AdminTopbar theme={theme} toggleTheme={toggleTheme} sidebarOpen={sidebarOpen} setSidebarOpen={handleToggleSidebar} />

        {/* Page Content */}
        <main className="admin-main-content" style={{ padding: '2rem', flexGrow: 1, overflowY: 'auto' }}>
          <div className="animate-fade-in max-w-7xl mx-auto">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
};

export default AdminLayout;
