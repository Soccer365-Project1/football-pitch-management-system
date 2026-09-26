import React from 'react';
import { createPortal } from 'react-dom';

/**
 * =========================================================================================
 * INTERFACE: ModalOverlayProps
 * =========================================================================================
 * Định nghĩa các thuộc tính (Props) truyền vào component ModalOverlay:
 * - children: Nội dung bất kỳ đặt bên trong hộp thoại (tiêu đề, form nhập liệu, nút bấm).
 * - onClose: Hàm callback được gọi khi người dùng click vào lớp nền mờ bên ngoài để đóng modal.
 * - maxWidth: Chiều rộng tối đa của hộp thoại (mặc định: '520px').
 */
export interface ModalOverlayProps {
  children: React.ReactNode;
  onClose: () => void;
  maxWidth?: string;
}

/**
 * =========================================================================================
 * COMPONENT: ModalOverlay (Khung Popup Nổi Toàn Cục)
 * =========================================================================================
 * Thành phần dùng chung cho toàn bộ các màn hình Quản trị (Sân bóng, Khung giờ, v.v.).
 * Sử dụng kỹ thuật React Portal (`createPortal`) để gắn trực tiếp thẻ modal vào `document.body`.
 * 
 * Lợi ích kỹ thuật cho Intern:
 * 1. Tránh hoàn toàn lỗi bị che khuất hoặc cắt góc (overflow clipping) bởi các khối container cha.
 * 2. Phủ một lớp nền đen mờ (`rgba(0, 0, 0, 0.55)`) toàn màn hình, tạo hiệu ứng tập trung cho người dùng.
 * 3. Hộp thoại ở giữa có chiều cao tối đa `90vh`, tự động cuộn dọc (`overflowY: auto`) nếu form dài.
 */
export const ModalOverlay: React.FC<ModalOverlayProps> = ({
  children,
  onClose,
  maxWidth = '520px',
}) => {
  return createPortal(
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.55)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 9999, // Đảm bảo luôn nằm trên tất cả các thành phần khác
        padding: '1.25rem',
      }}
    >
      {/* 
        Vùng nhấn backdrop trong suốt bao phủ toàn màn hình.
        Khi người dùng click chuột ra ngoài hộp thoại, sự kiện onClick này sẽ gọi onClose để đóng modal.
      */}
      <div
        style={{
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
        }}
        onClick={onClose}
      />

      {/* 
        Khung chứa nội dung chính của Modal (Hộp thoại màu trắng nổi ở giữa màn hình):
        - Class 'card' thừa hưởng bo góc và đổ bóng chuẩn từ file index.css
        - transform: 'none' để tránh hiệu ứng bay nhẹ khi hover chuột của class card gốc
      */}
      <div
        className="card"
        style={{
          position: 'relative',
          zIndex: 10000,
          width: '100%',
          maxWidth,
          maxHeight: '90vh',
          display: 'flex',
          flexDirection: 'column',
          transform: 'none',
          boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.25), 0 10px 10px -5px rgba(0, 0, 0, 0.1)',
          padding: '1.75rem',
          borderRadius: '16px',
        }}
      >
        {children}
      </div>
    </div>,
    document.body
  );
};

export default ModalOverlay;
