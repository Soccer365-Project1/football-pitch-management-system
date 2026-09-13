import React from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';

/**
 * ProtectedRoute: Route Guard bảo vệ các trang yêu cầu đăng nhập
 * - Đang F5 kiểm tra token (isLoading = true) -> Hiển thị spinner chờ
 * - Chưa đăng nhập -> Chuyển hướng về /login kèm location.state
 * - Đã đăng nhập -> Render trang con qua <Outlet />
 */
const ProtectedRoute: React.FC = () => {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-[50vh]">
        <div 
          className="animate-spin rounded-full h-8 w-8 border-b-2" 
          style={{ borderColor: 'var(--color-primary)' }}
        ></div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <Outlet />;
};

export default ProtectedRoute;
