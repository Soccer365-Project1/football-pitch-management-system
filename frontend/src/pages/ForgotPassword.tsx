import React from 'react';
import { Link } from 'react-router-dom';

const ForgotPassword: React.FC = () => {
  return (
    <div className="container mx-auto px-4 py-16 text-center animate-fade-in">
      <h1 className="text-3xl font-bold mb-4">Quên mật khẩu</h1>
      <p className="text-muted text-lg mb-8">Tính năng của trang Quên mật khẩu đang phát triển...</p>
      <Link to="/" className="btn btn-primary inline-flex items-center">
        Quay lại trang chủ
      </Link>
    </div>
  );
};

export default ForgotPassword;
