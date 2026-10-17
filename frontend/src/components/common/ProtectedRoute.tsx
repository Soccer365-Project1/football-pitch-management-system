import React from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import type { RoleName } from '../../types/adminUser';

interface ProtectedRouteProps {
  /**
   * Danh sách các vai trò được phép truy cập route này (tùy chọn).
   * Ví dụ: ['ROLE_ADMIN', 'ROLE_STAFF']
   */
  allowedRoles?: RoleName[];
  children?: React.ReactNode;
}

/**
 * ProtectedRoute: Route Guard bảo vệ các trang yêu cầu đăng nhập và phân quyền vai trò
 * - Đang F5 kiểm tra token (isLoading = true) -> Hiển thị spinner chờ
 * - Chưa đăng nhập -> Chuyển hướng về /login kèm location.state
 * - Đã đăng nhập nhưng không đủ quyền:
 *   + STAFF cố vào khu vực ADMIN -> Chuyển về /admin/timeline
 *   + CUSTOMER cố vào khu vực ADMIN/STAFF -> Chuyển về /
 * - Hợp lệ -> Render children hoặc <Outlet />
 */
const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ allowedRoles, children }) => {
  const { user, isAuthenticated, isLoading } = useAuth();
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

  // Kiểm tra phân quyền vai trò
  if (allowedRoles && user && !allowedRoles.includes(user.role)) {
    if (user.role === 'ROLE_STAFF') {
      return <Navigate to="/admin/timeline" replace />;
    }
    return <Navigate to="/" replace />;
  }

  return children ? <>{children}</> : <Outlet />;
};

export default ProtectedRoute;
