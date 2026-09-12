import React from 'react';
import { Link } from 'react-router-dom';
import { Sun, Moon, Menu } from 'lucide-react';

interface AdminTopbarProps {
  theme: 'light' | 'dark';
  toggleTheme: () => void;
  setSidebarOpen: (open: boolean) => void;
}

const AdminTopbar: React.FC<AdminTopbarProps> = ({ theme, toggleTheme, setSidebarOpen }) => {
  return (
    <header className="admin-header" style={{ height: '64px', flexShrink: 0, backgroundColor: 'var(--color-bg-surface)', borderBottom: '1px solid var(--color-border)', display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 2rem' }}>
      <div className="flex items-center gap-4">
        <button className="menu-toggle-btn btn btn-secondary" style={{ padding: '0.5rem', borderRadius: 'var(--radius-sm)' }} onClick={() => setSidebarOpen(true)}>
          <Menu size={20} />
        </button>
      </div>

      <div className="flex items-center gap-6">
        <button onClick={toggleTheme} className="btn btn-secondary" style={{ padding: '0.5rem', borderRadius: '50%' }} title="Đổi giao diện">
          {theme === 'light' ? <Moon size={20} /> : <Sun size={20} />}
        </button>

        <div style={{ width: '1px', height: '24px', backgroundColor: 'var(--color-border)' }}></div>

        <Link to="/admin/profile" style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', textDecoration: 'none' }}>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end' }}>
            <span className="font-bold text-sm" style={{ color: 'var(--color-text-base)' }}>Nguyễn Thu Ngân</span>
            <span className="text-xs text-muted font-medium">Quản trị viên</span>
          </div>
          <div style={{ width: '36px', height: '36px', borderRadius: '50%', backgroundColor: 'var(--color-primary)', color: 'white', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 'bold' }}>
            NN
          </div>
        </Link>
      </div>
    </header>
  );
};

export default AdminTopbar;
