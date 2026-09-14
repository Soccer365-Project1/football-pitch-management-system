import axios from 'axios';
import { showToast } from '../utils/toast';

/**
 * Lấy Base URL từ biến môi trường Vite (.env) hoặc fallback về cổng 8080 mặc định
 */
const getBaseUrl = (): string => {
  if (import.meta.env.VITE_API_BASE_URL) {
    return import.meta.env.VITE_API_BASE_URL;
  }
  if (import.meta.env.VITE_API_URL) {
    return `${import.meta.env.VITE_API_URL}/v1`;
  }
  return 'http://localhost:8080/api/v1';
};

/**
 * Axios instance cơ sở kết nối tới Backend Spring Boot
 */
const api = axios.create({
  baseURL: getBaseUrl(),
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

/**
 * 1. Request Interceptor:
 * Tự động kiểm tra và đính kèm JWT Bearer Token vào Header của mọi request bảo mật
 * Kiểm tra cả localStorage (Ghi nhớ) và sessionStorage (Tạm thời)
 */
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

/**
 * 2. Response Interceptor:
 * Xử lý lỗi 401 Unauthorized tập trung khi Token hết hạn hoặc không hợp lệ
 */
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Dọn dẹp sạch token ở cả 2 nơi lưu trữ
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      sessionStorage.removeItem('accessToken');
      sessionStorage.removeItem('refreshToken');

      // Tự động chuyển hướng về trang /login nếu người dùng đang ở trang khác
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login';
      }
    } else {
      // Global error handling with SweetAlert2
      const errorMessage = error.response?.data?.message || error.response?.data?.error || 'Có lỗi xảy ra, vui lòng thử lại!';
      showToast(errorMessage, 'error');
    }
    return Promise.reject(error);
  }
);

export default api;
