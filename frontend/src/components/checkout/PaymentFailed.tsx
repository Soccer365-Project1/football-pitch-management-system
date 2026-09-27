import React from 'react';
import { XCircle, RefreshCw } from 'lucide-react';
import { formatPrice } from '../../utils/formatUtils.ts';

interface PaymentFailedProps {
  pitch: any;
  slot: any;
  depositAmount: number;
  onRetry: () => void;
  onBookMore: () => void;
}

const PaymentFailed: React.FC<PaymentFailedProps> = ({ pitch, slot, depositAmount, onRetry, onBookMore }) => (
  <div className="flex justify-center mt-8 px-4">
    <div className="card text-center shadow-xl animate-fade-in" style={{ maxWidth: 520, width: '100%', border: '1.5px solid var(--color-danger)' }}>
      <div className="w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4" style={{ backgroundColor: 'rgba(239, 68, 68, 0.12)', color: 'var(--color-danger)' }}>
        <XCircle size={40} />
      </div>
      <h2 className="text-2xl font-bold mb-2 text-danger">Thanh toán không thành công!</h2>
      <p className="text-muted mb-6">Giao dịch thanh toán tiền cọc chưa được hoàn tất hoặc đã bị hủy.</p>

      <div className="p-4 mb-6 text-left" style={{ backgroundColor: 'var(--color-bg-base)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border)' }}>
        <div className="flex justify-between mb-2">
          <span className="text-muted">Sân bóng:</span>
          <span className="font-semibold">{pitch.name}</span>
        </div>
        <div className="flex justify-between mb-2">
          <span className="text-muted">Khung giờ:</span>
          <span className="font-semibold">{slot.startTime} - {slot.endTime}</span>
        </div>
        <div className="flex justify-between mb-2">
          <span className="text-muted">Tiền cọc cần thanh toán:</span>
          <span className="font-bold text-danger">{formatPrice(depositAmount)}</span>
        </div>
        <div className="flex justify-between pt-2 border-t text-sm" style={{ borderColor: 'var(--color-border)' }}>
          <span className="text-muted">Lý do thất bại:</span>
          <span className="font-medium text-danger">Giao dịch bị từ chối / Hết thời gian chờ thanh toán</span>
        </div>
      </div>

      <div className="flex flex-col gap-5">
        <button className="btn btn-primary w-full py-3 flex items-center justify-center gap-2" onClick={onRetry}>
          <RefreshCw size={18} /> Thử thanh toán lại
        </button>
        <button className="btn btn-secondary w-full" onClick={onBookMore}>
          Đặt sân khác
        </button>
      </div>
    </div>
  </div>
);

export default PaymentFailed;
