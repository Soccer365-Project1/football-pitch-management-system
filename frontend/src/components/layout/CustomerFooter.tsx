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
          <Link to="/terms" aria-label="Xem điều khoản dịch vụ Soccer365">Điều khoản sử dụng</Link>
          <Link to="/privacy" aria-label="Xem chính sách bảo mật thông tin Soccer365">Chính sách bảo mật</Link>
          <Link to="/contact" aria-label="Thông tin liên hệ và hỗ trợ Soccer365">Hỗ trợ & Liên hệ</Link>
        </div>
      </div>
    </footer>
  );
};

export default CustomerFooter;
