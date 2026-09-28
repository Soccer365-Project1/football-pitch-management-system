/**
 * Cấu trúc chi tiết của lỗi validation từng trường
 * Tương ứng với class FieldErrorDetail.java bên Backend
 */
export interface FieldErrorDetail {
  field: string;
  message: string;
  rejectedValue?: any;
}

/**
 * Cấu trúc phản hồi chuẩn của toàn bộ hệ thống API Backend
 * Tương ứng với class ApiResponse<T> bên Java Spring Boot
 * 
 * - success: true nếu xử lý thành công, false nếu có lỗi
 * - code: Mã trạng thái HTTP (200, 201, 400, 401, 403...) hoặc mã lỗi nghiệp vụ
 * - message: Thông điệp phản hồi từ server (ví dụ: "Đăng ký tài khoản thành công")
 * - data: Dữ liệu trả về (Generic type T)
 * - errors: Danh sách các trường dữ liệu bị lỗi validation nếu có
 * - timestamp: Thời gian server xử lý request
 */
export interface ApiResponse<T = void> {
  success?: boolean;
  code: number;
  message: string;
  data?: T;
  errors?: FieldErrorDetail[];
  timestamp?: string;
}
