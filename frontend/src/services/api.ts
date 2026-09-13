import axios from 'axios';

/**
 * Axios instance cơ sở kết nối tới Backend Spring Boot
 * 
 * - baseURL: Địa chỉ gốc của API Backend (chạy ở cổng 8080, prefix /api/v1)
 * - headers: Định dạng gửi và nhận mặc định là JSON
 * - timeout: Giới hạn thời gian chờ phản hồi tối đa là 10 giây
 */
const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

export default api;
