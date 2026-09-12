import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Calendar, CreditCard, Settings, Users, LogOut, BarChart3, X } from 'lucide-react';

interface AdminSidebarProps {
  sidebarOpen: boolean;
  setSidebarOpen: (open: boolean) => void;
}

const AdminSidebar: React.FC<AdminSidebarProps> = ({ sidebarOpen, setSidebarOpen }) => {
  const location = useLocation();

  const menuItems = [
    { path: '/admin', icon: <BarChart3 size={20} />, label: 'Thống kê' },
    { path: '/admin/timeline', icon: <Calendar size={20} />, label: 'Quản lý Ca Đá' },
    { path: '/admin/bookings', icon: <CreditCard size={20} />, label: 'Quản lý Đơn' },
    { path: '/admin/transactions', icon: <CreditCard size={20} />, label: 'Giao dịch' },
    { path: '/admin/users', icon: <Users size={20} />, label: 'Người dùng' },
    { path: '/admin/pitches', icon: <Settings size={20} />, label: 'Quản lý Sân' },
    { path: '/admin/timeslots', icon: <Calendar size={20} />, label: 'Khung giờ' },
    { path: '/admin/pricing', icon: <CreditCard size={20} />, label: 'Bảng giá sân' },
  ];

  return (
    <>
      {/* Sidebar Overlay (Mobile/Tablet) */}
      {sidebarOpen && (
        <div className="admin-sidebar-overlay" onClick={() => setSidebarOpen(false)}></div>
      )}

      {/* Sidebar */}
      <aside className={`admin-sidebar ${sidebarOpen ? 'open' : ''}`} style={{ width: '250px', flexShrink: 0, backgroundColor: 'var(--color-bg-surface)', borderRight: '1px solid var(--color-border)', display: 'flex', flexDirection: 'column' }}>
        <div style={{ padding: '1.5rem', borderBottom: '1px solid var(--color-border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <h2 className="text-2xl font-bold" style={{ color: 'var(--color-primary)' }}>Soccer365</h2>
            <p className="text-muted text-sm mt-1">Phần mềm quản lý sân bóng</p>
          </div>
          <button className="menu-toggle-btn text-muted" onClick={() => setSidebarOpen(false)}>
            <X size={24} />
          </button>
        </div>

        <nav style={{ padding: '1rem 0', flexGrow: 1 }}>
          {menuItems.map(item => {
            const isActive = location.pathname === item.path || (item.path !== '/admin' && location.pathname.startsWith(item.path));
            return (
              <Link
                key={item.path}
                to={item.path}
                style={{
                  display: 'flex', alignItems: 'center', gap: '0.75rem', padding: '0.75rem 1.5rem',
                  backgroundColor: isActive ? 'var(--color-primary-light)' : 'transparent',
                  color: isActive ? 'var(--color-primary)' : 'var(--color-text-base)',
                  borderRight: isActive ? '3px solid var(--color-primary)' : '3px solid transparent',
                  fontWeight: isActive ? 600 : 500,
                  transition: 'all var(--transition-fast)'
                }}
                onClick={() => setSidebarOpen(false)}
              >
                {item.icon} {item.label}
              </Link>
            );
          })}
        </nav>

        <div style={{ padding: '1rem', borderTop: '1px solid var(--color-border)' }}>
          <Link to="/" className="btn w-full" style={{ justifyContent: 'flex-start', color: 'var(--color-danger)', padding: '0.875rem 1rem', fontSize: '1rem' }}>
            <LogOut size={20} /> Đăng xuất
          </Link>
        </div>
      </aside>
    </>
  );
};

export default AdminSidebar;
