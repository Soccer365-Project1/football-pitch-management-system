import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { CheckCircle2, XCircle, AlertTriangle, ArrowRight, RefreshCw, Calendar } from 'lucide-react';
import { bookingService, type PaymentReturnResult } from '../services/bookingService.ts';
import { formatPrice } from '../utils/formatUtils.ts';
import { useDocumentTitle } from '../hooks/useDocumentTitle.ts';

const PaymentResult: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [result, setResult] = useState<PaymentReturnResult | null>(null);
  const [repaying, setRepaying] = useState(false);
  const [repayError, setRepayError] = useState<string | null>(null);

  useDocumentTitle(
    'Kết Quả Thanh Toán Đặt Sân | Soccer365',
    'Thông tin xác thực giao dịch thanh toán đặt sân bóng đá qua cổng thanh toán VNPAY trên Soccer365.'
  );

  useEffect(() => {
    let isMounted = true;
    const verifyPayment = async () => {
      try {
        const queryString = searchParams.toString();
        if (!queryString) {
          if (isMounted) {
            setResult({
              status: 'FAILED',
              message: 'Không tìm thấy tham số kết quả giao dịch.',
            });
            setLoading(false);
          }
          return;
        }

        const res = await bookingService.verifyPaymentReturn(queryString);
        if (isMounted) {
          setResult(res.data);
        }
      } catch (err: any) {
        if (isMounted) {
          setResult({
            status: 'FAILED',
            message: err?.response?.data?.message || 'Không thể xác thực giao dịch từ máy chủ.',
          });
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    verifyPayment();
    return () => {
      isMounted = false;
    };
  }, [searchParams]);

  if (loading) {
    return (
      <div className="min-h-[calc(100vh-160px)] flex flex-col items-center justify-center p-4" role="status" aria-live="polite">
        <div className="w-12 h-12 border-4 border-primary border-t-transparent rounded-full animate-spin mb-4"></div>
        <p className="text-muted font-medium text-base">Đang xác thực kết quả thanh toán từ VNPAY...</p>
        <span className="sr-only">Đang xác thực kết quả thanh toán...</span>
      </div>
    );
  }

  const isSuccess = result?.status === 'SUCCESS';
  const isRemainingPayment =
    result?.paymentType === 'REMAINING_70' || result?.status === 'REMAINING_CANCELLED';
  const isPendingHold = result?.status === 'PENDING_HOLD';
  const isRemainingCancelled = result?.status === 'REMAINING_CANCELLED';
  const isRetryableWarning = isPendingHold || isRemainingCancelled;
  const isCancelled = result?.status === 'CANCELLED';

  const handleRepay = async () => {
    if (!result?.bookingId) return;
    try {
      setRepaying(true);
      setRepayError(null);
      const res = isRemainingPayment
        ? await bookingService.payRemainingBooking(result.bookingId)
        : await bookingService.repayBooking(result.bookingId);
      if (res.data?.paymentUrl) {
        window.location.href = res.data.paymentUrl;
      }
    } catch (err: any) {
      setRepayError(
        err?.response?.data?.message ||
          (isRemainingPayment
            ? 'Không thể tạo liên kết thanh toán lại. Vui lòng thử lại từ trang Đơn của tôi.'
            : 'Không thể tạo liên kết thanh toán lại. Có thể thời gian giữ chỗ đã hết hạn.')
      );
    } finally {
      setRepaying(false);
    }
  };

  return (
    <div className="min-h-[calc(100vh-160px)] flex items-center justify-center p-4">
      <main
        className="card max-w-lg w-full text-center p-8 shadow-xl border border-border"
        style={{
          borderRadius: 'var(--radius-lg)',
          backgroundColor: 'var(--color-bg-surface)',
        }}
        aria-label="Kết quả thanh toán đơn đặt sân"
      >
        <div className="mb-4 flex justify-center">
          <img
            src="https://sandbox.vnpayment.vn/paymentv2/Images/brands/logo.svg"
            alt="Logo cổng thanh toán điện tử VNPAY-QR"
            width="110"
            height="32"
            loading="lazy"
            style={{ height: '32px', width: 'auto', aspectRatio: '110/32', objectFit: 'contain' }}
          />
        </div>

        {isSuccess ? (
          <>
            <div
              className="w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4"
              style={{ backgroundColor: 'rgba(16, 185, 129, 0.12)', color: '#047857' }}
              aria-hidden="true"
            >
              <CheckCircle2 size={42} />
            </div>
            <h1 className="text-2xl font-bold mb-2">Thanh toán thành công!</h1>
            <p className="text-muted text-sm mb-6">
              {result?.message || 'Khung giờ sân bóng của bạn đã được đảm bảo và gửi tới ban quản lý.'}
            </p>

            <div
              className="rounded-lg p-4 text-left text-sm space-y-2 mb-6"
              style={{
                backgroundColor: 'var(--color-bg-base)',
                border: '1px solid var(--color-border)',
              }}
            >
              <div className="flex justify-between items-center py-1">
                <span className="text-muted">Mã đơn đặt sân:</span>
                <span className="font-semibold">{result?.bookingCode || '---'}</span>
              </div>
              <div className="flex justify-between items-center py-1">
                <span className="text-muted">Số tiền đã thanh toán:</span>
                <span className="font-bold text-primary">
                  {result?.amount ? formatPrice(result.amount) : '---'}
                </span>
              </div>
              {result?.bankCode && (
                <div className="flex justify-between items-center py-1">
                  <span className="text-muted">Ngân hàng thanh toán:</span>
                  <span className="font-medium">{result.bankCode}</span>
                </div>
              )}
              {result?.transactionNo && (
                <div className="flex justify-between items-center py-1">
                  <span className="text-muted">Mã GD VNPAY:</span>
                  <span className="font-mono text-xs">{result.transactionNo}</span>
                </div>
              )}
            </div>

            <div className="flex flex-col gap-3">
              <button
                type="button"
                onClick={() => navigate('/my-bookings')}
                className="btn btn-primary w-full flex items-center justify-center gap-2 py-3 cursor-pointer"
                style={{ padding: '0.85rem' }}
                aria-label="Xem danh sách đơn đặt sân của tôi"
              >
                <Calendar size={18} aria-hidden="true" />
                <span>Xem danh sách đơn của tôi</span>
                <ArrowRight size={18} aria-hidden="true" />
              </button>
              <button
                type="button"
                onClick={() => navigate('/book-pitch')}
                className="btn btn-outline w-full py-3 cursor-pointer"
                style={{ padding: '0.85rem' }}
                aria-label="Tiếp tục tra cứu và đặt thêm sân khác"
              >
                Đặt thêm sân khác
              </button>
            </div>
          </>
        ) : isRetryableWarning ? (
          <>
            <div
              className="w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4"
              style={{ backgroundColor: 'rgba(245, 158, 11, 0.15)', color: '#b45309' }}
              aria-hidden="true"
            >
              <AlertTriangle size={42} />
            </div>
            <h1 className="text-2xl font-bold mb-2">
              {isRemainingCancelled ? 'Đã hủy thanh toán nốt tiền sân' : 'Thanh toán chưa hoàn tất'}
            </h1>
            <p className="text-muted text-sm mb-6">
              {result?.message ||
                (isRemainingCancelled
                  ? 'Bạn đã hủy giao dịch thanh toán phần tiền còn lại trên VNPAY. Đơn đặt sân vẫn đang hoạt động, bạn có thể thử thanh toán lại.'
                  : 'Giao dịch trên VNPAY chưa hoàn thành, nhưng khung giờ của bạn vẫn đang được tạm giữ. Bạn có thể thanh toán lại để giữ chỗ.')}
            </p>

            {repayError && (
              <div
                className="p-3 mb-4 rounded-lg text-sm text-left font-medium"
                style={{ backgroundColor: 'rgba(239, 68, 68, 0.1)', color: '#b91c1c', border: '1px solid rgba(239, 68, 68, 0.2)' }}
                role="alert"
              >
                {repayError}
              </div>
            )}

            {result?.bookingCode && (
              <div
                className="rounded-lg p-4 text-left text-sm space-y-2 mb-6"
                style={{
                  backgroundColor: 'var(--color-bg-base)',
                  border: '1px solid var(--color-border)',
                }}
              >
                <div className="flex justify-between items-center py-1">
                  <span className="text-muted">Mã đơn đặt sân:</span>
                  <span className="font-semibold">{result.bookingCode}</span>
                </div>
                {result.amount != null && (
                  <div className="flex justify-between items-center py-1">
                    <span className="text-muted">
                      {isRemainingCancelled ? 'Số tiền còn lại cần thanh toán (70%):' : 'Tiền cọc cần thanh toán (30%):'}
                    </span>
                    <span className="font-bold text-primary">{formatPrice(result.amount)}</span>
                  </div>
                )}
              </div>
            )}

            <div className="flex flex-col gap-3">
              <button
                type="button"
                onClick={handleRepay}
                disabled={repaying}
                className="btn btn-primary w-full flex items-center justify-center gap-2 py-3 cursor-pointer"
                style={{ padding: '0.85rem' }}
                aria-label="Thanh toán lại qua cổng VNPAY"
              >
                {repaying ? (
                  <>
                    <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    <span>Đang tạo liên kết thanh toán...</span>
                  </>
                ) : (
                  <>
                    <RefreshCw size={18} aria-hidden="true" />
                    <span>{isRemainingCancelled ? 'Thanh toán lại nốt tiền sân (VNPAY)' : 'Thanh toán lại cọc VNPAY'}</span>
                  </>
                )}
              </button>
              <button
                type="button"
                onClick={() => navigate('/my-bookings')}
                className="btn btn-outline w-full py-3 cursor-pointer"
                style={{ padding: '0.85rem' }}
                aria-label="Về danh sách đơn đặt sân cá nhân"
              >
                Về danh sách đơn của tôi
              </button>
            </div>
          </>
        ) : isCancelled ? (
          <>
            <div
              className="w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4"
              style={{ backgroundColor: 'rgba(239, 68, 68, 0.12)', color: '#b91c1c' }}
              aria-hidden="true"
            >
              <AlertTriangle size={42} />
            </div>
            <h1 className="text-2xl font-bold mb-2">Đã hủy đơn đặt sân</h1>
            <p className="text-muted text-sm mb-6">
              Giao dịch đã bị hủy và thời gian giữ chỗ đã hết hạn. Khung giờ sân đã được giải phóng.
            </p>
            <div className="flex flex-col gap-3">
              <button
                type="button"
                onClick={() => navigate('/book-pitch')}
                className="btn btn-primary w-full flex items-center justify-center gap-2 py-3 cursor-pointer"
                style={{ padding: '0.85rem' }}
                aria-label="Chọn lại khung giờ và đặt lại sân bóng"
              >
                <RefreshCw size={18} aria-hidden="true" />
                <span>Chọn lại khung giờ & Đặt lại</span>
              </button>
              <button
                type="button"
                onClick={() => navigate('/my-bookings')}
                className="btn btn-outline w-full py-3 cursor-pointer"
                style={{ padding: '0.85rem' }}
                aria-label="Về trang quản lý danh sách đơn"
              >
                Về trang quản lý đơn
              </button>
            </div>
          </>
        ) : (
          <>
            <div
              className="w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4"
              style={{ backgroundColor: 'rgba(239, 68, 68, 0.12)', color: '#b91c1c' }}
              aria-hidden="true"
            >
              <XCircle size={42} />
            </div>
            <h1 className="text-2xl font-bold mb-2">
              {isRemainingPayment ? 'Thanh toán nốt tiền sân không thành công' : 'Thanh toán không thành công'}
            </h1>
            <p className="text-muted text-sm mb-6">
              {result?.message ||
                (isRemainingPayment
                  ? 'Giao dịch thanh toán phần tiền còn lại gặp sự cố. Bạn có thể thử thanh toán lại hoặc thanh toán trực tiếp tại sân.'
                  : 'Giao dịch đặt cọc gặp sự cố hoặc đã hết hạn giữ chỗ.')}
            </p>
            {repayError && (
              <div
                className="p-3 mb-4 rounded-lg text-sm text-left font-medium"
                style={{ backgroundColor: 'rgba(239, 68, 68, 0.1)', color: '#b91c1c', border: '1px solid rgba(239, 68, 68, 0.2)' }}
                role="alert"
              >
                {repayError}
              </div>
            )}
            <div className="flex flex-col gap-3">
              {isRemainingPayment && result?.bookingId ? (
                <>
                  <button
                    type="button"
                    onClick={handleRepay}
                    disabled={repaying}
                    className="btn btn-primary w-full flex items-center justify-center gap-2 py-3 cursor-pointer"
                    style={{ padding: '0.85rem' }}
                    aria-label="Thanh toán lại qua cổng VNPAY"
                  >
                    <RefreshCw size={18} aria-hidden="true" />
                    <span>Thanh toán lại qua VNPAY</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => navigate('/my-bookings')}
                    className="btn btn-outline w-full py-3 cursor-pointer"
                    style={{ padding: '0.85rem' }}
                    aria-label="Về danh sách đơn đặt sân"
                  >
                    Về danh sách đơn của tôi
                  </button>
                </>
              ) : (
                <>
                  <button
                    type="button"
                    onClick={() => navigate('/book-pitch')}
                    className="btn btn-primary w-full flex items-center justify-center gap-2 py-3 cursor-pointer"
                    style={{ padding: '0.85rem' }}
                    aria-label="Thử chọn và đặt lại sân"
                  >
                    <RefreshCw size={18} aria-hidden="true" />
                    <span>Thử đặt lại</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => navigate('/')}
                    className="btn btn-outline w-full py-3 cursor-pointer"
                    style={{ padding: '0.85rem' }}
                    aria-label="Quay về trang chủ Soccer365"
                  >
                    Quay về trang chủ
                  </button>
                </>
              )}
            </div>
          </>
        )}
      </main>
    </div>
  );
};

export default PaymentResult;
