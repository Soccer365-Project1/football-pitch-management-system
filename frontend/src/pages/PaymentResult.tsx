import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { CheckCircle2, XCircle, AlertTriangle, ArrowRight, RefreshCw, Calendar } from 'lucide-react';
import { bookingService, type PaymentReturnResult } from '../services/bookingService.ts';
import { formatPrice } from '../utils/formatUtils.ts';

const PaymentResult: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [result, setResult] = useState<PaymentReturnResult | null>(null);
  const [repaying, setRepaying] = useState(false);
  const [repayError, setRepayError] = useState<string | null>(null);

  useEffect(() => {
    const verifyPayment = async () => {
      try {
        const queryString = searchParams.toString();
        if (!queryString) {
          setResult({
            status: 'FAILED',
            message: 'Không tìm thấy tham số kết quả giao dịch.',
          });
          setLoading(false);
          return;
        }

        const res = await bookingService.verifyPaymentReturn(queryString);
        setResult(res.data);
      } catch (err: any) {
        setResult({
          status: 'FAILED',
          message: err?.response?.data?.message || 'Không thể xác thực giao dịch từ máy chủ.',
        });
      } finally {
        setLoading(false);
      }
    };

    verifyPayment();
  }, [searchParams]);

  if (loading) {
    return (
      <div className="min-h-[calc(100vh-160px)] flex flex-col items-center justify-center p-4">
        <div className="w-12 h-12 border-4 border-primary border-t-transparent rounded-full animate-spin mb-4"></div>
        <p className="text-muted font-medium text-base">Đang xác thực kết quả thanh toán từ VNPAY...</p>
      </div>
    );
  }

  const handleRepay = async () => {
    if (!result?.bookingId) return;
    try {
      setRepaying(true);
      setRepayError(null);
      const res = await bookingService.repayBooking(result.bookingId);
      if (res.data?.paymentUrl) {
        window.location.href = res.data.paymentUrl;
      }
    } catch (err: any) {
      setRepayError(err?.response?.data?.message || 'Không thể tạo liên kết thanh toán lại. Có thể thời gian giữ chỗ đã hết hạn.');
    } finally {
      setRepaying(false);
    }
  };

  const isSuccess = result?.status === 'SUCCESS';
  const isPendingHold = result?.status === 'PENDING_HOLD';
  const isCancelled = result?.status === 'CANCELLED';

  return (
    <div className="min-h-[calc(100vh-160px)] flex items-center justify-center p-4">
      <div
        className="card max-w-lg w-full text-center p-8 shadow-xl border border-border"
        style={{
          borderRadius: 'var(--radius-lg)',
          backgroundColor: 'var(--color-bg-surface)',
        }}
      >
        {isSuccess ? (
          <>
            <div
              className="w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4"
              style={{ backgroundColor: 'rgba(16, 185, 129, 0.1)', color: '#10b981' }}
            >
              <CheckCircle2 size={42} />
            </div>
            <h1 className="text-2xl font-bold mb-2">Đặt cọc thành công!</h1>
            <p className="text-muted text-sm mb-6">
              {result?.message || 'Khung giờ của bạn đã được đảm bảo và gửi tới quản lý sân.'}
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
                <span className="text-muted">Tiền cọc đã thanh toán (30%):</span>
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
                onClick={() => navigate('/my-bookings')}
                className="btn btn-primary w-full flex items-center justify-center gap-2 py-3"
                style={{ padding: '0.85rem' }}
              >
                <Calendar size={18} /> Xem danh sách đơn của tôi <ArrowRight size={18} />
              </button>
              <button
                onClick={() => navigate('/book-pitch')}
                className="btn btn-outline w-full py-3"
                style={{ padding: '0.85rem' }}
              >
                Đặt thêm sân khác
              </button>
            </div>
          </>
        ) : isPendingHold ? (
          <>
            <div
              className="w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4"
              style={{ backgroundColor: 'rgba(245, 158, 11, 0.1)', color: '#f59e0b' }}
            >
              <AlertTriangle size={42} />
            </div>
            <h1 className="text-2xl font-bold mb-2">Thanh toán chưa hoàn tất</h1>
            <p className="text-muted text-sm mb-6">
              {result?.message || 'Giao dịch trên VNPAY chưa hoàn thành, nhưng khung giờ của bạn vẫn đang được tạm giữ. Bạn có thể thanh toán lại để giữ chỗ.'}
            </p>

            {repayError && (
              <div
                className="p-3 mb-4 rounded-lg text-sm text-left"
                style={{ backgroundColor: 'rgba(239, 68, 68, 0.1)', color: '#ef4444', border: '1px solid rgba(239, 68, 68, 0.2)' }}
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
                {result.amount && (
                  <div className="flex justify-between items-center py-1">
                    <span className="text-muted">Tiền cọc cần thanh toán (30%):</span>
                    <span className="font-bold text-primary">{formatPrice(result.amount)}</span>
                  </div>
                )}
              </div>
            )}

            <div className="flex flex-col gap-3">
              <button
                onClick={handleRepay}
                disabled={repaying}
                className="btn btn-primary w-full flex items-center justify-center gap-2 py-3"
                style={{ padding: '0.85rem' }}
              >
                {repaying ? (
                  <>
                    <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    Đang tạo liên kết thanh toán...
                  </>
                ) : (
                  <>
                    <RefreshCw size={18} /> Thanh toán lại cọc VNPAY
                  </>
                )}
              </button>
              <button
                onClick={() => navigate('/my-bookings')}
                className="btn btn-outline w-full py-3"
                style={{ padding: '0.85rem' }}
              >
                Về danh sách đơn của tôi
              </button>
            </div>
          </>
        ) : isCancelled ? (
          <>
            <div
              className="w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4"
              style={{ backgroundColor: 'rgba(239, 68, 68, 0.1)', color: '#ef4444' }}
            >
              <AlertTriangle size={42} />
            </div>
            <h1 className="text-2xl font-bold mb-2">Đã hủy đơn đặt sân</h1>
            <p className="text-muted text-sm mb-6">
              Giao dịch đã bị hủy và thời gian giữ chỗ đã hết hạn. Khung giờ sân đã được giải phóng.
            </p>
            <div className="flex flex-col gap-3">
              <button
                onClick={() => navigate('/book-pitch')}
                className="btn btn-primary w-full flex items-center justify-center gap-2 py-3"
                style={{ padding: '0.85rem' }}
              >
                <RefreshCw size={18} /> Chọn lại khung giờ & Đặt lại
              </button>
              <button
                onClick={() => navigate('/my-bookings')}
                className="btn btn-outline w-full py-3"
                style={{ padding: '0.85rem' }}
              >
                Về trang quản lý đơn
              </button>
            </div>
          </>
        ) : (
          <>
            <div
              className="w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4"
              style={{ backgroundColor: 'rgba(239, 68, 68, 0.1)', color: '#ef4444' }}
            >
              <XCircle size={42} />
            </div>
            <h1 className="text-2xl font-bold mb-2">Thanh toán không thành công</h1>
            <p className="text-muted text-sm mb-6">
              {result?.message || 'Giao dịch đặt cọc gặp sự cố hoặc đã hết hạn.'}
            </p>
            <div className="flex flex-col gap-3">
              <button
                onClick={() => navigate('/book-pitch')}
                className="btn btn-primary w-full flex items-center justify-center gap-2 py-3"
                style={{ padding: '0.85rem' }}
              >
                <RefreshCw size={18} /> Thử đặt lại
              </button>
              <button
                onClick={() => navigate('/')}
                className="btn btn-outline w-full py-3"
                style={{ padding: '0.85rem' }}
              >
                Quay về trang chủ
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default PaymentResult;
