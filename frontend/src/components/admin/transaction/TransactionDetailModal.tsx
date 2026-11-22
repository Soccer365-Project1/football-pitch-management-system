import React, { useState, useEffect } from 'react';
import { X, Loader2, CreditCard, Calendar, User, FileText, CheckCircle2, Clock, AlertTriangle, Copy, Check } from 'lucide-react';
import { ModalOverlay } from '../../common/ModalOverlay';
import { transactionService, type TransactionDetailResponse } from '../../../services/transactionService';
import { formatPrice, formatDate } from '../../../utils/formatUtils';
import { showToast } from '../../../utils/toast';

export interface TransactionDetailModalProps {
  isOpen: boolean;
  transactionId: number | null;
  onClose: () => void;
}

export const TransactionDetailModal: React.FC<TransactionDetailModalProps> = ({
  isOpen,
  transactionId,
  onClose,
}) => {
  const [loading, setLoading] = useState<boolean>(false);
  const [detail, setDetail] = useState<TransactionDetailResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [copiedCode, setCopiedCode] = useState<boolean>(false);

  useEffect(() => {
    if (!isOpen || !transactionId) {
      setDetail(null);
      setError(null);
      setCopiedCode(false);
      return;
    }

    let isMounted = true;
    setLoading(true);
    setError(null);

    transactionService
      .getTransactionDetail(transactionId)
      .then((data) => {
        if (isMounted) setDetail(data);
      })
      .catch((err) => {
        if (isMounted) {
          setError(err?.response?.data?.message || 'Không thể tải chi tiết giao dịch thanh toán');
        }
      })
      .finally(() => {
        if (isMounted) setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [isOpen, transactionId]);

  if (!isOpen) return null;

  const handleCopy = (text?: string) => {
    if (!text) return;
    navigator.clipboard.writeText(text);
    setCopiedCode(true);
    showToast('Đã sao chép mã giao dịch vào bộ nhớ tạm', 'success');
    setTimeout(() => setCopiedCode(false), 2000);
  };

  const getStatusBadge = (status?: string) => {
    switch (status) {
      case 'SUCCESS':
        return (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.35rem',
              padding: '0.25rem 0.65rem',
              borderRadius: '9999px',
              fontSize: '0.75rem',
              fontWeight: 600,
              backgroundColor: '#ecfdf5',
              color: '#047857',
              border: '1px solid #a7f3d0',
            }}
          >
            <CheckCircle2 size={13} color="#10b981" />
            Thành công
          </span>
        );
      case 'PENDING':
        return (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.35rem',
              padding: '0.25rem 0.65rem',
              borderRadius: '9999px',
              fontSize: '0.75rem',
              fontWeight: 600,
              backgroundColor: '#fffbeb',
              color: '#b45309',
              border: '1px solid #fde68a',
            }}
          >
            <Clock size={13} color="#f59e0b" />
            Đang chờ xử lý
          </span>
        );
      case 'FAILED':
        return (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.35rem',
              padding: '0.25rem 0.65rem',
              borderRadius: '9999px',
              fontSize: '0.75rem',
              fontWeight: 600,
              backgroundColor: '#fef2f2',
              color: '#b91c1c',
              border: '1px solid #fecaca',
            }}
          >
            <AlertTriangle size={13} color="#ef4444" />
            Thất bại
          </span>
        );
      default:
        return (
          <span
            style={{
              padding: '0.25rem 0.65rem',
              borderRadius: '9999px',
              fontSize: '0.75rem',
              backgroundColor: 'var(--color-bg-base)',
              color: 'var(--color-text-muted)',
            }}
          >
            {status}
          </span>
        );
    }
  };

  const getPaymentTypeLabel = (type?: string) => {
    switch (type) {
      case 'DEPOSIT_30':
        return (
          <span style={{ padding: '0.2rem 0.55rem', borderRadius: '0.375rem', fontSize: '0.75rem', fontWeight: 500, backgroundColor: '#eff6ff', color: '#1d4ed8', border: '1px solid #bfdbfe', whiteSpace: 'nowrap', display: 'inline-block' }}>
            Cọc (30%)
          </span>
        );
      case 'REMAINING_70':
        return (
          <span style={{ padding: '0.2rem 0.55rem', borderRadius: '0.375rem', fontSize: '0.75rem', fontWeight: 500, backgroundColor: '#eef2ff', color: '#4338ca', border: '1px solid #c7d2fe', whiteSpace: 'nowrap', display: 'inline-block' }}>
            Còn lại (70%)
          </span>
        );
      case 'FULL_PAYMENT':
        return (
          <span style={{ padding: '0.2rem 0.55rem', borderRadius: '0.375rem', fontSize: '0.75rem', fontWeight: 500, backgroundColor: '#ecfdf5', color: '#047857', border: '1px solid #a7f3d0', whiteSpace: 'nowrap', display: 'inline-block' }}>
            Toàn bộ (100%)
          </span>
        );
      case 'REFUND':
        return (
          <span style={{ padding: '0.2rem 0.55rem', borderRadius: '0.375rem', fontSize: '0.75rem', fontWeight: 500, backgroundColor: '#faf5ff', color: '#7e22ce', border: '1px solid #e9d5ff', whiteSpace: 'nowrap', display: 'inline-block' }}>
            Hoàn tiền cọc
          </span>
        );
      default:
        return <span style={{ fontSize: '0.75rem', whiteSpace: 'nowrap' }}>{type}</span>;
    }
  };

  const formatDateTime = (iso?: string) => {
    if (!iso) return '---';
    const d = new Date(iso);
    return d.toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  };

  return (
    <ModalOverlay onClose={onClose} maxWidth="680px">
      {/* 1. Header Modal */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          paddingBottom: '0.85rem',
          marginBottom: '1rem',
          borderBottom: '1px solid var(--color-border)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <CreditCard style={{ color: 'var(--color-primary)' }} size={22} />
            <h2 style={{ fontSize: '1.2rem', fontWeight: 800, margin: 0, color: 'var(--color-text-base)' }}>
              Chi Tiết Giao Dịch
            </h2>
          </div>
          {detail && (
            <div
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.35rem',
                padding: '0.2rem 0.55rem',
                backgroundColor: 'var(--color-bg-base)',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-md)',
                fontFamily: 'monospace',
                fontSize: '0.75rem',
                color: 'var(--color-text-muted)',
              }}
            >
              <span>{detail.transactionCode}</span>
              <button
                type="button"
                onClick={() => handleCopy(detail.transactionCode)}
                style={{
                  background: 'none',
                  border: 'none',
                  cursor: 'pointer',
                  padding: 0,
                  display: 'flex',
                  alignItems: 'center',
                  color: 'var(--color-text-muted)',
                }}
                title="Sao chép mã giao dịch"
              >
                {copiedCode ? <Check size={12} color="#10b981" /> : <Copy size={12} />}
              </button>
            </div>
          )}
          {detail && getStatusBadge(detail.status)}
        </div>
        <button
          onClick={onClose}
          style={{
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            padding: '0.25rem',
            borderRadius: 'var(--radius-md)',
            color: 'var(--color-text-muted)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
          title="Đóng"
        >
          <X size={20} />
        </button>
      </div>

      {/* 2. Nội dung chi tiết */}
      {loading && (
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '3rem 1rem', color: 'var(--color-text-muted)', gap: '0.5rem' }}>
          <Loader2 className="animate-spin" size={32} style={{ color: 'var(--color-primary)' }} />
          <span style={{ fontSize: '0.875rem' }}>Đang tải thông tin giao dịch...</span>
        </div>
      )}

      {error && (
        <div style={{ padding: '1rem', borderRadius: 'var(--radius-md)', backgroundColor: '#fef2f2', border: '1px solid #fecaca', color: '#b91c1c', fontSize: '0.875rem' }}>
          {error}
        </div>
      )}

      {!loading && !error && detail && (
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            gap: '1rem',
            maxHeight: 'calc(80vh - 120px)',
            overflowY: 'auto',
            paddingRight: '0.35rem',
          }}
        >
          {/* Card nổi bật: Số tiền & Phương thức */}
          <div
            style={{
              padding: '1.25rem',
              borderRadius: 'var(--radius-md)',
              backgroundColor: 'var(--color-bg-base)',
              border: '1px solid var(--color-border)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              flexWrap: 'wrap',
              gap: '1rem',
            }}
          >
            <div>
              <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--color-text-muted)', textTransform: 'uppercase' }}>
                Số tiền giao dịch
              </span>
              <div
                style={{
                  fontSize: '1.75rem',
                  fontWeight: 900,
                  marginTop: '0.2rem',
                  color: detail.paymentType === 'REFUND' ? '#dc2626' : '#047857',
                }}
              >
                {detail.paymentType === 'REFUND' ? '-' : '+'}
                {formatPrice(detail.amount)}
              </div>
              <div style={{ marginTop: '0.4rem', display: 'flex', alignItems: 'center', gap: '0.45rem', flexWrap: 'wrap' }}>
                {getPaymentTypeLabel(detail.paymentType)}
                <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>qua</span>
                <span
                  style={{
                    fontSize: '0.75rem',
                    fontWeight: 600,
                    padding: '0.15rem 0.5rem',
                    borderRadius: 'var(--radius-md)',
                    backgroundColor: 'var(--color-bg-surface)',
                    border: '1px solid var(--color-border)',
                    color: 'var(--color-text-base)',
                  }}
                >
                  {detail.paymentMethodName || detail.paymentMethodCode || 'Chưa xác định'}
                </span>
              </div>
            </div>
            <div>{getStatusBadge(detail.status)}</div>
          </div>

          {/* Khối 1: Thông tin Đối soát & Kỹ thuật */}
          <div
            style={{
              padding: '1rem 1.15rem',
              borderRadius: 'var(--radius-md)',
              backgroundColor: 'var(--color-bg-surface)',
              border: '1px solid var(--color-border)',
            }}
          >
            <div
              style={{
                fontSize: '0.75rem',
                fontWeight: 700,
                color: 'var(--color-primary)',
                textTransform: 'uppercase',
                letterSpacing: '0.04em',
                display: 'flex',
                alignItems: 'center',
                gap: '0.4rem',
                marginBottom: '0.75rem',
              }}
            >
              <FileText size={15} />
              Thông Tin Đối Soát & Kỹ Thuật
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '0.75rem', fontSize: '0.85rem' }}>
              <div>
                <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', display: 'block' }}>Mã đối soát Ngân hàng / Cổng VNPAY:</span>
                <span style={{ fontFamily: 'monospace', fontWeight: 600, color: 'var(--color-text-base)' }}>
                  {detail.referenceCode || <span style={{ color: 'var(--color-text-muted)', fontStyle: 'italic', fontWeight: 400 }}>Không có mã đối soát</span>}
                </span>
              </div>
              <div>
                <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', display: 'block' }}>Thời gian tạo:</span>
                <span style={{ fontWeight: 600, color: 'var(--color-text-base)' }}>{formatDateTime(detail.createdAt)}</span>
              </div>
              <div style={{ gridColumn: '1 / -1' }}>
                <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', display: 'block' }}>Ghi chú giao dịch:</span>
                <span style={{ color: 'var(--color-text-base)' }}>
                  {detail.note || <span style={{ color: 'var(--color-text-muted)', fontStyle: 'italic' }}>Không có ghi chú</span>}
                </span>
              </div>
            </div>
          </div>

          {/* Khối 2: Thông tin Đơn đặt sân liên kết */}
          {detail.bookingId ? (
            <div
              style={{
                padding: '1rem 1.15rem',
                borderRadius: 'var(--radius-md)',
                backgroundColor: 'var(--color-bg-surface)',
                border: '1px solid var(--color-border)',
              }}
            >
              <div
                style={{
                  fontSize: '0.75rem',
                  fontWeight: 700,
                  color: 'var(--color-primary)',
                  textTransform: 'uppercase',
                  letterSpacing: '0.04em',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.4rem',
                  marginBottom: '0.75rem',
                }}
              >
                <Calendar size={15} />
                Đơn Đặt Sân Liên Kết
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '0.75rem', fontSize: '0.85rem' }}>
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', display: 'block' }}>Mã đơn đặt:</span>
                  <span style={{ fontFamily: 'monospace', fontWeight: 800, color: 'var(--color-primary)' }}>
                    {detail.bookingCode || `#B${detail.bookingId}`}
                  </span>
                </div>
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', display: 'block' }}>Trạng thái đơn:</span>
                  <span style={{ fontWeight: 600, color: 'var(--color-text-base)' }}>{detail.bookingStatus || '---'}</span>
                </div>
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', display: 'block' }}>Sân bóng & Khung giờ:</span>
                  <span style={{ fontWeight: 600, color: 'var(--color-text-base)' }}>
                    {detail.pitchName || '---'} ({detail.timeSlot || '---'})
                  </span>
                </div>
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', display: 'block' }}>Ngày đá:</span>
                  <span style={{ fontWeight: 600, color: 'var(--color-text-base)' }}>
                    {detail.bookingDate ? formatDate(detail.bookingDate) : '---'}
                  </span>
                </div>
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', display: 'block' }}>Khách hàng:</span>
                  <span style={{ fontWeight: 600, color: 'var(--color-text-base)' }}>
                    {detail.customerName || 'Khách vãng lai'} {detail.customerPhone ? `(${detail.customerPhone})` : ''}
                  </span>
                </div>
                <div>
                  <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', display: 'block' }}>Tổng tiền sân / Tiền cọc:</span>
                  <span style={{ fontWeight: 600, color: 'var(--color-text-base)' }}>
                    {detail.bookingTotalAmount ? formatPrice(detail.bookingTotalAmount) : '---'} /{' '}
                    {detail.bookingDepositAmount ? formatPrice(detail.bookingDepositAmount) : '0 đ'}
                  </span>
                </div>
              </div>
            </div>
          ) : (
            <div
              style={{
                padding: '1rem',
                borderRadius: 'var(--radius-md)',
                border: '1px dashed var(--color-border)',
                textAlign: 'center',
                color: 'var(--color-text-muted)',
                fontSize: '0.85rem',
              }}
            >
              Giao dịch độc lập (không gắn với đơn đặt sân cụ thể)
            </div>
          )}

          {/* Khối 3: Người xử lý trực tiếp */}
          <div
            style={{
              padding: '1rem 1.15rem',
              borderRadius: 'var(--radius-md)',
              backgroundColor: 'var(--color-bg-surface)',
              border: '1px solid var(--color-border)',
            }}
          >
            <div
              style={{
                fontSize: '0.75rem',
                fontWeight: 700,
                color: 'var(--color-primary)',
                textTransform: 'uppercase',
                letterSpacing: '0.04em',
                display: 'flex',
                alignItems: 'center',
                gap: '0.4rem',
                marginBottom: '0.75rem',
              }}
            >
              <User size={15} />
              Kênh Thực Hiện & Người Xử Lý
            </div>
            <div style={{ fontSize: '0.85rem' }}>
              {detail.processedByName ? (
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                  <div
                    style={{
                      width: '36px',
                      height: '36px',
                      borderRadius: '9999px',
                      backgroundColor: 'rgba(16, 185, 129, 0.1)',
                      color: 'var(--color-primary)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontWeight: 700,
                      fontSize: '0.9rem',
                    }}
                  >
                    {detail.processedByName.charAt(0).toUpperCase()}
                  </div>
                  <div>
                    <div style={{ fontWeight: 700, color: 'var(--color-text-base)' }}>{detail.processedByName}</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
                      {detail.processedByEmail || 'Thu ngân tại quầy'}
                    </div>
                  </div>
                </div>
              ) : (
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--color-text-muted)', fontStyle: 'italic' }}>
                  <span style={{ width: '8px', height: '8px', borderRadius: '9999px', backgroundColor: '#10b981', display: 'inline-block' }} />
                  Giao dịch trực tuyến tự động qua Cổng thanh toán (Không qua thu ngân)
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* 3. Footer Modal */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'flex-end',
          paddingTop: '0.85rem',
          marginTop: '1rem',
          borderTop: '1px solid var(--color-border)',
        }}
      >
        <button
          type="button"
          onClick={onClose}
          style={{
            padding: '0.5rem 1.25rem',
            fontSize: '0.875rem',
            fontWeight: 600,
            color: 'var(--color-text-base)',
            backgroundColor: 'var(--color-bg-base)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-md)',
            cursor: 'pointer',
            transition: 'background-color 0.15s ease',
          }}
          onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = 'var(--color-border)')}
          onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'var(--color-bg-base)')}
        >
          Đóng
        </button>
      </div>
    </ModalOverlay>
  );
};

export default TransactionDetailModal;
