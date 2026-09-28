import React from 'react';
import { CreditCard } from 'lucide-react';
import { formatPrice } from '../../utils/formatUtils.ts';
import { useAuth } from '../../contexts/AuthContext';

interface CheckoutFormProps {
  depositAmount: number;
  isProcessing: boolean;
  onPaymentSuccess: () => void;
  onPaymentFailed: () => void;
}

const CheckoutForm: React.FC<CheckoutFormProps> = ({ depositAmount, isProcessing, onPaymentSuccess, onPaymentFailed }) => {
  const { user } = useAuth();
  
  return (
  <div className="card flex flex-col checkout-form">
    <h2 className="text-xl font-semibold mb-4 pb-2" style={{ borderBottom: '1px solid var(--color-border)' }}>Thông tin người đặt</h2>
    <form className="flex flex-col gap-4 flex-grow">
      <div className="grid gap-4" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))' }}>
        <div>
          <label className="font-semibold text-sm">Họ và tên (*)</label>
          <input type="text" className="mt-2 w-full" defaultValue={user?.fullName || ''} style={{ padding: '0.75rem', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border)', backgroundColor: 'var(--color-bg-base)', color: 'var(--color-text-base)' }} placeholder="Nhập họ tên..." />
        </div>
        <div>
          <label className="font-semibold text-sm">Số điện thoại (*)</label>
          <input type="text" className="mt-2 w-full" defaultValue={user?.phoneNumber || ''} style={{ padding: '0.75rem', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border)', backgroundColor: 'var(--color-bg-base)', color: 'var(--color-text-base)' }} placeholder="Nhập số điện thoại..." />
        </div>
      </div>
      <div className="flex-grow flex flex-col">
        <label className="font-semibold text-sm">Ghi chú (Tùy chọn)</label>
        <textarea className="mt-2 w-full flex-grow mb-4" style={{ padding: '0.75rem', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border)', backgroundColor: 'var(--color-bg-base)', color: 'var(--color-text-base)', minHeight: '100px' }} placeholder="Yêu cầu thêm (VD: thuê bóng, áo bib, nước suối...)" />
      </div>

      <div className="p-4 rounded-lg text-sm mb-4" style={{ backgroundColor: 'rgba(59, 130, 246, 0.05)', border: '1px dashed var(--color-border)' }}>
        <strong style={{ color: 'var(--color-primary)' }}>Lưu ý:</strong> Vui lòng hoàn tất thanh toán cọc trong vòng 10 phút để giữ khung giờ sân.
      </div>

      <div className="flex flex-col gap-2 mt-auto">
        <button
          type="button"
          className="btn btn-primary w-full flex items-center justify-center gap-2"
          style={{ padding: '1rem', fontSize: '1rem' }}
          onClick={onPaymentSuccess}
          disabled={isProcessing}
        >
          {isProcessing ? 'Đang kết nối cổng thanh toán...' : <><CreditCard size={20} /> Thanh toán cọc {formatPrice(depositAmount)}</>}
        </button>

        <button
          type="button"
          className="btn btn-secondary w-full text-xs py-2"
          onClick={onPaymentFailed}
          disabled={isProcessing}
          title="Mô phỏng trường hợp cổng thanh toán trả về lỗi"
        >
          [ Mô phỏng: Thanh toán thất bại / Lỗi ]
        </button>
      </div>

      <p className="text-xs text-center text-muted mt-1">
        Bằng việc nhấn Thanh toán, bạn đồng ý với Điều khoản đặt sân của Soccer365.
      </p>
    </form>
  </div>
);
};

export default CheckoutForm;
