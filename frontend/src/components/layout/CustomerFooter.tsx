import React from 'react';
import { Link } from 'react-router-dom';

const CustomerFooter: React.FC = () => {
  return (
    <footer className="modern-footer">
      <div className="container text-sm text-muted">
        <div className="flex items-center gap-2 font-bold" style={{ color: 'var(--color-primary)' }}>
          Soccer365 © {new Date().getFullYear()}
        </div>
        <div className="text-center md:text-left font-medium">
          Hệ thống Quản lý và Đặt sân bóng đá trực tuyến
        </div>
        <div className="flex items-center gap-6 font-medium">
          <Link to="/terms">Điều khoản</Link>
          <Link to="/privacy">Bảo mật</Link>
          <Link to="/contact">Liên hệ</Link>
        </div>
      </div>
    </footer>
  );
};

export default CustomerFooter;
