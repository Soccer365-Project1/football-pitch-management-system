import React from 'react';
import { formatPrice, formatDate } from '../../utils/formatUtils.ts';

interface CheckoutInvoiceProps {
  pitch: any;
  slot: any;
  depositAmount: number;
  remainingAmount: number;
  selectedDate: string;
  basePrice: number;
}

const CheckoutInvoice: React.FC<CheckoutInvoiceProps> = ({ pitch, slot, depositAmount, remainingAmount, selectedDate, basePrice }) => (
  <div className="card flex flex-col justify-between checkout-invoice">
    <h2 className="text-xl font-semibold mb-4 pb-2" style={{ borderBottom: '1px solid var(--color-border)' }}>Chi tiết đơn đặt sân</h2>

    <div className="flex flex-col gap-2 mb-6 p-4 rounded-lg overflow-hidden" style={{ backgroundColor: 'var(--color-bg-base)' }}>
      <h3 className="font-bold text-lg truncate" style={{ color: 'var(--color-primary)' }} title={pitch.name}>{pitch.name}</h3>
      <span className="text-muted text-sm truncate">{pitch.pitchType?.name || 'Sân bóng'}</span>
    </div>

    <div className="flex justify-between mb-3 text-sm items-center">
      <span className="text-muted">Ngày đá:</span>
      <span className="font-semibold px-2 py-1 bg-base rounded">{formatDate(selectedDate)}</span>
    </div>
    <div className="flex justify-between mb-3 text-sm items-center">
      <span className="text-muted">Khung giờ:</span>
      <span className="font-bold">{slot.startTime} - {slot.endTime}</span>
    </div>
    <div className="flex justify-between mb-3 text-sm items-center">
      <span className="text-muted">Loại sân:</span>
      <span className="font-semibold">{pitch.pitchType?.name || 'Sân bóng'}</span>
    </div>

    <hr style={{ borderColor: 'var(--color-border)', margin: '1.5rem 0' }} />

    <div className="flex justify-between mb-3 text-sm">
      <span className="text-muted">Tiền thuê sân:</span>
      <span className="font-semibold">{formatPrice(basePrice)}</span>
    </div>

    {slot.isPeakHour && (
      <div className="flex justify-between mb-3 text-sm">
        <span className="text-muted">Phụ phí giờ vàng:</span>
        <span className="badge badge-warning">Đã bao gồm</span>
      </div>
    )}

    <div className="flex justify-between mt-4 mb-2 p-4 rounded-lg" style={{ backgroundColor: 'rgba(16, 185, 129, 0.1)', border: '1px solid rgba(16, 185, 129, 0.2)' }}>
      <span className="font-semibold" style={{ color: 'var(--color-primary)' }}>Cần đặt cọc (30%):</span>
      <span className="font-bold text-xl" style={{ color: 'var(--color-primary)' }}>{formatPrice(depositAmount)}</span>
    </div>
    <div className="flex justify-between mt-4 mb-2 px-2 text-sm">
      <span className="text-muted">Còn lại thanh toán tại sân:</span>
      <span className="font-semibold text-warning">{formatPrice(remainingAmount)}</span>
    </div>
  </div>
);

export default CheckoutInvoice;
