import React from 'react';
import { CreditCard, Loader2 } from 'lucide-react';
import { formatPrice } from '../../utils/formatUtils.ts';
import { useAuth } from '../../contexts/AuthContext';

interface CheckoutFormProps {
  depositAmount: number;
  isProcessing: boolean;
  onSubmit: (formData: { guestName: string; guestPhone: string; customerNote: string }) => void;
}

const CheckoutForm: React.FC<CheckoutFormProps> = ({ depositAmount, isProcessing, onSubmit }) => {
  const { user } = useAuth();

  const [guestName, setGuestName] = React.useState(user?.fullName || '');
  const [guestPhone, setGuestPhone] = React.useState(user?.phoneNumber || '');
  const [customerNote, setCustomerNote] = React.useState('');

  const [nameError, setNameError] = React.useState('');
  const [phoneError, setPhoneError] = React.useState('');

  const validate = () => {
    let isValid = true;
    setNameError('');
    setPhoneError('');

    if (!guestName.trim()) {
      setNameError('Vui lòng nhập họ và tên');
      isValid = false;
    }

    if (!guestPhone.trim()) {
      setPhoneError('Vui lòng nhập số điện thoại');
      isValid = false;
    } else if (!/^(0[35789])[0-9]{8}$/.test(guestPhone.trim())) {
      setPhoneError('Số điện thoại không đúng định dạng (VD: 0912345678)');
      isValid = false;
    }

    return isValid;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (validate()) {
      onSubmit({ guestName: guestName.trim(), guestPhone: guestPhone.trim(), customerNote: customerNote.trim() });
    }
  };

  return (
    <section className="card flex flex-col checkout-form" aria-label="Biểu mẫu thông tin người đặt">
      <div className="flex items-center justify-between mb-4 pb-2 border-b" style={{ borderBottomColor: 'var(--color-border)' }}>
        <h2 className="text-xl font-semibold">Thông tin người đặt</h2>
        <img
          src="https://sandbox.vnpayment.vn/paymentv2/Images/brands/logo.svg"
          alt="Cổng thanh toán điện tử trực tuyến VNPAY-QR"
          width="90"
          height="26"
          loading="lazy"
          style={{ height: '26px', width: 'auto', aspectRatio: '90/26', objectFit: 'contain' }}
        />
      </div>

      <form className="flex flex-col gap-4 flex-grow" onSubmit={handleSubmit} noValidate>
        <div className="grid gap-4" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))' }}>
          <div>
            <label htmlFor="checkout-guest-name" className="font-semibold text-sm block mb-1">
              Họ và tên <span className="text-red-600" aria-hidden="true">*</span>
            </label>
            <input
              id="checkout-guest-name"
              type="text"
              required
              aria-required="true"
              aria-invalid={!!nameError}
              aria-describedby={nameError ? 'name-error-msg' : undefined}
              className={`w-full ${nameError ? 'border-red-500' : ''}`}
              value={guestName}
              onChange={(e) => {
                setGuestName(e.target.value);
                if (nameError) setNameError('');
              }}
              style={{
                padding: '0.75rem',
                borderRadius: 'var(--radius-md)',
                border: nameError ? '1px solid #ef4444' : '1px solid var(--color-border)',
                backgroundColor: 'var(--color-bg-base)',
                color: 'var(--color-text-base)',
              }}
              placeholder="Nhập họ và tên người đặt..."
            />
            {nameError && (
              <p id="name-error-msg" className="text-red-600 text-xs mt-1 font-semibold" role="alert">
                {nameError}
              </p>
            )}
          </div>

          <div>
            <label htmlFor="checkout-guest-phone" className="font-semibold text-sm block mb-1">
              Số điện thoại <span className="text-red-600" aria-hidden="true">*</span>
            </label>
            <input
              id="checkout-guest-phone"
              type="tel"
              required
              aria-required="true"
              aria-invalid={!!phoneError}
              aria-describedby={phoneError ? 'phone-error-msg' : undefined}
              className={`w-full ${phoneError ? 'border-red-500' : ''}`}
              value={guestPhone}
              onChange={(e) => {
                setGuestPhone(e.target.value);
                if (phoneError) setPhoneError('');
              }}
              style={{
                padding: '0.75rem',
                borderRadius: 'var(--radius-md)',
                border: phoneError ? '1px solid #ef4444' : '1px solid var(--color-border)',
                backgroundColor: 'var(--color-bg-base)',
                color: 'var(--color-text-base)',
              }}
              placeholder="Nhập số điện thoại liên hệ..."
            />
            {phoneError && (
              <p id="phone-error-msg" className="text-red-600 text-xs mt-1 font-semibold" role="alert">
                {phoneError}
              </p>
            )}
          </div>
        </div>

        <div className="flex-grow flex flex-col">
          <label htmlFor="checkout-customer-note" className="font-semibold text-sm block mb-1">
            Ghi chú đơn đặt (Tùy chọn)
          </label>
          <textarea
            id="checkout-customer-note"
            className="w-full flex-grow mb-4"
            value={customerNote}
            onChange={(e) => setCustomerNote(e.target.value)}
            style={{
              padding: '0.75rem',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--color-border)',
              backgroundColor: 'var(--color-bg-base)',
              color: 'var(--color-text-base)',
              minHeight: '90px',
            }}
            placeholder="Yêu cầu thêm nếu có (VD: mượn áo bib, thuê bóng, nước suối...)"
          />
        </div>

        <div
          className="p-4 rounded-lg text-sm mb-4"
          style={{ backgroundColor: 'rgba(59, 130, 246, 0.08)', border: '1px dashed var(--color-border)' }}
        >
          <strong style={{ color: 'var(--color-primary)' }}>Lưu ý giữ chỗ:</strong> Sau khi nhấn xác nhận, bạn có 10 phút để thanh toán cọc qua cổng VNPAY-QR để hoàn tất việc giữ sân.
        </div>

        <div className="flex flex-col gap-2 mt-auto">
          <button
            type="submit"
            className="btn btn-primary w-full flex items-center justify-center gap-2 cursor-pointer"
            style={{ padding: '0.9rem', fontSize: '1rem' }}
            disabled={isProcessing}
            aria-label={`Xác nhận đặt cọc ${formatPrice(depositAmount)} qua cổng VNPAY`}
          >
            {isProcessing ? (
              <>
                <Loader2 size={20} className="animate-spin" aria-hidden="true" />
                <span>Đang kết nối cổng VNPAY...</span>
              </>
            ) : (
              <>
                <CreditCard size={20} aria-hidden="true" />
                <span>Thanh toán đặt cọc {formatPrice(depositAmount)} qua VNPAY</span>
              </>
            )}
          </button>
        </div>
      </form>
    </section>
  );
};

export default CheckoutForm;
