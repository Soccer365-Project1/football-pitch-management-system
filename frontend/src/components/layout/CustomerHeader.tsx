import React, { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Moon, Sun, User, LogOut, Menu, X } from 'lucide-react';

interface CustomerHeaderProps {
  theme: 'light' | 'dark';
  toggleTheme: () => void;
  isLoggedIn: boolean;
  handleLogout: () => void;
  showUserMenu: boolean;
  isProfilePage: boolean;
}

const CustomerHeader: React.FC<CustomerHeaderProps> = ({
  theme,
  toggleTheme,
  isLoggedIn,
  handleLogout,
  showUserMenu,
  isProfilePage
}) => {
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const location = useLocation();

  useEffect(() => {
    // Close mobile menu when route changes
    setIsMobileMenuOpen(false);
  }, [location]);

  return (
    <header className="navbar">
      <div className="container flex items-center justify-between">
        <Link to="/" className="font-bold text-2xl" style={{ color: 'var(--color-primary)' }}>
          Soccer365
        </Link>
        
        {/* Desktop Navigation */}
        <nav className="hidden md:flex items-center gap-6">
          <Link to="/" className="font-semibold">Trang Chủ</Link>
          <Link to="/book-pitch" className="font-semibold">Đặt Sân</Link>
          <Link to="/my-bookings" className="font-semibold">Đơn Của Tôi</Link>
          
          <div className="flex items-center gap-4">
            <button onClick={toggleTheme} className="btn btn-secondary" style={{ padding: '0.5rem', borderRadius: '50%' }} title="Chuyển đổi giao diện">
              {theme === 'light' ? <Moon size={20} /> : <Sun size={20} />}
            </button>
            
            {showUserMenu ? (
              <>
                <Link 
                  to="/profile" 
                  className={`btn ${isProfilePage ? 'btn-primary' : 'btn-secondary'} flex items-center gap-2 font-semibold`} 
                  style={{ padding: '0 1rem', height: '40px', justifyContent: 'center' }}
                >
                  <User size={18} />
                  <span>Hồ Sơ</span>
                </Link>
                <button 
                  onClick={handleLogout} 
                  className="btn btn-secondary flex items-center gap-1.5 font-semibold" 
                  style={{ padding: '0 1rem', height: '40px', justifyContent: 'center', color: 'var(--color-danger)' }}
                  title="Đăng xuất"
                >
                  <LogOut size={18} />
                  <span>Đăng xuất</span>
                </button>
              </>
            ) : (
              <>
                <Link to="/register" className="btn btn-secondary font-semibold" style={{ padding: '0.5rem 1.2rem' }}>
                  Đăng ký
                </Link>
                <Link to="/login" className="btn btn-primary font-semibold" style={{ padding: '0.5rem 1.2rem' }}>
                  Đăng nhập
                </Link>
              </>
            )}
          </div>
        </nav>

        {/* Mobile Menu Button */}
        <div className="flex items-center gap-2 md:hidden">
          <button onClick={toggleTheme} className="btn btn-secondary" style={{ padding: '0.5rem', borderRadius: '50%' }}>
            {theme === 'light' ? <Moon size={18} /> : <Sun size={18} />}
          </button>
          <button 
            className="btn btn-secondary" 
            style={{ padding: '0.5rem' }} 
            onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
          >
            {isMobileMenuOpen ? <X size={20} /> : <Menu size={20} />}
          </button>
        </div>
      </div>

      {/* Mobile Dropdown */}
      {isMobileMenuOpen && (
        <div className="md:hidden border-t" style={{ borderColor: 'var(--color-border)', backgroundColor: 'var(--color-bg-surface)' }}>
          <div className="container flex flex-col gap-3 py-4">
            <Link to="/" className="font-semibold py-2">Trang Chủ</Link>
            <Link to="/book-pitch" className="font-semibold py-2">Đặt Sân</Link>
            <Link to="/my-bookings" className="font-semibold py-2">Đơn Của Tôi</Link>
            {showUserMenu && <Link to="/profile" className="font-semibold py-2">Hồ Sơ Của Tôi</Link>}
            
            <div className="pt-2 border-t flex flex-col gap-2" style={{ borderColor: 'var(--color-border)' }}>
              {showUserMenu ? (
                <button 
                  onClick={handleLogout} 
                  className="btn btn-secondary font-semibold w-full flex items-center justify-center gap-2"
                  style={{ color: 'var(--color-danger)', padding: '0.75rem' }}
                >
                  <LogOut size={20} />
                  <span>Đăng xuất</span>
                </button>
              ) : (
                <>
                  <Link to="/login" className="btn btn-primary font-semibold w-full">
                    Đăng nhập
                  </Link>
                  <Link to="/register" className="btn btn-secondary font-semibold w-full">
                    Đăng ký
                  </Link>
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </header>
  );
};

export default CustomerHeader;
