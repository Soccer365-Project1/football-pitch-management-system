import React from 'react';
import { Link } from 'react-router-dom';

const CustomerFooter: React.FC = () => {
  return (
    <footer className="modern-footer">
      <div className="container text-sm">
        <div className="flex items-center gap-2 font-bold" style={{ color: 'var(--color-primary)' }}>
          Soccer365 © {new Date().getFullYear()}
        </div>
        <div className="text-center md:text-left font-medium text-muted">
          Hệ thống Quản lý và Đặt sân bóng đá trực tuyến
        </div>
        <div className="flex items-center gap-6 font-medium">
          <Link to="/terms" aria-label="Điều khoản sử dụng">Điều khoản</Link>
          <Link to="/privacy" aria-label="Chính sách bảo mật">Bảo mật</Link>
          <Link to="/contact" aria-label="Thông tin liên hệ">Liên hệ</Link>
        </div>
      </div>
    </footer>
  );
};

export default CustomerFooter;
