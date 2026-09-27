import React from 'react';
import { CheckCircle } from 'lucide-react';
import { formatPrice } from '../../utils/formatUtils.ts';

interface PaymentSuccessProps {
  pitch: any;
  slot: any;
  depositAmount: number;
  remainingAmount: number;
  onViewBookings: () => void;
  onBookMore: () => void;
}

const PaymentSuccess: React.FC<PaymentSuccessProps> = ({ pitch, slot, depositAmount, remainingAmount, onViewBookings, onBookMore }) => (
  <div className="flex justify-center mt-8 px-4">
    <div className="card text-center shadow-xl animate-fade-in" style={{ maxWidth: 520, width: '100%', border: '1.5px solid var(--color-border)' }}>
      <div className="w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4" style={{ backgroundColor: 'rgba(34, 197, 94, 0.12)', color: 'var(--color-success)' }}>
        <CheckCircle size={40} />
      </div>
      <h2 className="text-2xl font-bold mb-2">Thanh toán cọc thành công!</h2>
      <p className="text-muted mb-6">Đơn đặt sân của bạn đã được xác nhận vào hệ thống (CONFIRMED).</p>

      <div className="p-4 mb-6 text-left" style={{ backgroundColor: 'var(--color-bg-base)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border)' }}>
        <div className="flex justify-between mb-2">
          <span className="text-muted">Sân bóng:</span>
          <span className="font-semibold">{pitch.name} ({pitch.pitchType?.name || 'Sân bóng'})</span>
        </div>
        <div className="flex justify-between mb-2">
          <span className="text-muted">Khung giờ:</span>
          <span className="font-semibold">{slot.startTime} - {slot.endTime}</span>
        </div>
        <div className="flex justify-between mb-2">
          <span className="text-muted">Tiền cọc đã thanh toán (30%):</span>
          <span className="font-bold text-primary">{formatPrice(depositAmount)}</span>
        </div>
        <div className="flex justify-between pt-2 border-t" style={{ borderColor: 'var(--color-border)' }}>
          <span className="text-muted">Số tiền còn lại (trả tại sân):</span>
          <span className="font-semibold text-warning">{formatPrice(remainingAmount)}</span>
        </div>
      </div>

      <div className="flex flex-col gap-3">
        <button className="btn btn-primary w-full py-3" onClick={onViewBookings}>
          Xem Đơn Đặt Sân Của Tôi
        </button>
        <button className="btn btn-secondary w-full" onClick={onBookMore}>
          Đặt thêm sân khác
        </button>
      </div>
    </div>
  </div>
);

export default PaymentSuccess;
