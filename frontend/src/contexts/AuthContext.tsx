import React, { createContext, useContext, useState, useEffect } from 'react';
import type { UserResponse, LoginRequest } from '../types/auth';
import { authService } from '../services/authService';

interface AuthContextType {
  user: UserResponse | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (data: LoginRequest, rememberMe?: boolean) => Promise<UserResponse>;
  loginWithGoogle: (idToken: string) => Promise<UserResponse>;
  logout: () => void;
  updateUser: (updatedUser: UserResponse) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

/**
 * AuthProvider: Component bọc ngoài ứng dụng để chia sẻ phiên đăng nhập cho toàn bộ website
 */
export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  // Tự động kiểm tra token khi vừa mở web hoặc người dùng F5 tải lại trang
  // Kiểm tra cả localStorage (Ghi nhớ) và sessionStorage (Tạm thời)
  useEffect(() => {
    const initAuth = async () => {
      const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
      if (token) {
        try {
          // getMeApi tự động được Interceptor đính kèm token trong header
          const res = await authService.getMeApi();
          if (res.data) {
            setUser(res.data);
          }
        } catch (error) {
          console.error("Token không hợp lệ hoặc đã hết hạn:", error);
          authService.logout();
          setUser(null);
        }
      }
      setIsLoading(false);
    };

    initAuth();
  }, []);

  // Cập nhật thông tin User trong Context
  const updateUser = (updatedUser: UserResponse) => {
    setUser(updatedUser);
  };

  // Xử lý Đăng nhập tập trung có hỗ trợ "Ghi nhớ" (rememberMe)
  const login = async (data: LoginRequest, rememberMe: boolean = false): Promise<UserResponse> => {
    setIsLoading(true);
    try {
      const res = await authService.loginApi(data);
      const authData = res.data;

      if (!authData) {
        throw new Error('Dữ liệu phản hồi từ máy chủ không hợp lệ');
      }

      // Xử lý lưu trữ theo cờ rememberMe
      if (rememberMe) {
        // Tích "Ghi nhớ": Lưu dài hạn vào localStorage
        localStorage.setItem('accessToken', authData.accessToken);
        localStorage.setItem('refreshToken', authData.refreshToken);
        sessionStorage.removeItem('accessToken');
        sessionStorage.removeItem('refreshToken');
      } else {
        // Không tích "Ghi nhớ": Lưu tạm thời vào sessionStorage (tắt trình duyệt tự mất token)
        sessionStorage.setItem('accessToken', authData.accessToken);
        sessionStorage.setItem('refreshToken', authData.refreshToken);
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
      }

      // Cập nhật state User để toàn bộ component trên màn hình tự đổi giao diện
      setUser(authData.user);
      return authData.user;
    } finally {
      setIsLoading(false);
    }
  };

  // Xử lý Đăng nhập qua Google ID Token
  const loginWithGoogle = async (idToken: string): Promise<UserResponse> => {
    setIsLoading(true);
    try {
      const res = await authService.loginWithGoogleApi(idToken);
      const authData = res.data;

      if (!authData) {
        throw new Error('Dữ liệu xác thực Google không hợp lệ');
      }

      // Tài khoản Google mặc định lưu lâu dài vào localStorage
      localStorage.setItem('accessToken', authData.accessToken);
      localStorage.setItem('refreshToken', authData.refreshToken);

      setUser(authData.user);
      return authData.user;
    } finally {
      setIsLoading(false);
    }
  };

  // Xử lý Đăng xuất tập trung
  const logout = () => {
    authService.logout();
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        loginWithGoogle,
        logout,
        updateUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

/**
 * Custom Hook useAuth(): Giúp bất kỳ component nào chỉ cần 1 dòng code là đọc được phiên đăng nhập
 */
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth phải được sử dụng bên trong AuthProvider');
  }
  return context;
};
