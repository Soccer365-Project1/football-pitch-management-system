import React, { useState } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { usePitches, useTimeSlots, useScheduleGrid } from '../hooks/queries/usePitchQueries.ts';

import CheckoutHeader from '../components/checkout/CheckoutHeader.tsx';
import CheckoutInvoice from '../components/checkout/CheckoutInvoice.tsx';
import CheckoutForm from '../components/checkout/CheckoutForm.tsx';
import { bookingService } from '../services/bookingService.ts';
import { showToast } from '../utils/toast.ts';
import { useDocumentTitle } from '../hooks/useDocumentTitle.ts';

const Checkout: React.FC = () => {
  const { pitchId, timeSlotId } = useParams<{ pitchId: string; timeSlotId: string }>();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  useDocumentTitle(
    'Xác Nhận Đặt Sân & Đặt Cọc Trực Tuyến | Soccer365',
    'Xác nhận thông tin đặt sân bóng đá và thanh toán tiền cọc 30% bảo mật an toàn qua cổng thanh toán điện tử VNPAY.'
  );

  const selectedDate = searchParams.get('date') || new Date().toISOString().split('T')[0];

  const { data: pitches = [], isLoading: isLoadingPitches } = usePitches('all');
  const { data: timeSlots = [], isLoading: isLoadingSlots } = useTimeSlots();
  const { data: scheduleData = { bookings: [], prices: [] }, isLoading: isLoadingSchedule } = useScheduleGrid(
    selectedDate,
    'all'
  );

  const [isProcessing, setIsProcessing] = useState(false);

  if (isLoadingPitches || isLoadingSlots || isLoadingSchedule) {
    return (
      <div className="flex justify-center items-center py-12" role="status" aria-live="polite">
        <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
        <span className="sr-only">Đang tải thông tin đơn đặt sân...</span>
      </div>
    );
  }

  const pitch = pitches.find((p) => p.id === Number(pitchId));
  const slot = timeSlots.find((t) => t.id === Number(timeSlotId));

  if (!pitch || !slot) {
    return (
      <div className="text-center mt-8 text-danger font-semibold" role="alert">
        Không tìm thấy thông tin sân hoặc khung giờ! Vui lòng quay lại kiểm tra.
      </div>
    );
  }

  const pitchPriceItem = (scheduleData.prices || []).find(
    (p: any) => p.pitchTypeId === pitch.pitchType?.id && p.isPeakHour === slot.isPeakHour
  );
  const basePrice = pitchPriceItem ? pitchPriceItem.price : slot.isPeakHour ? 300000 : 250000;

  const depositRatio = 0.3; // 30% cọc
  const depositAmount = basePrice * depositRatio;
  const remainingAmount = basePrice - depositAmount;

  const handleBookingSubmit = async (formData: { guestName: string; guestPhone: string; customerNote: string }) => {
    setIsProcessing(true);
    try {
      const response = await bookingService.checkoutDeposit({
        pitchId: Number(pitchId),
        timeSlotId: Number(timeSlotId),
        bookingDate: selectedDate,
        ...formData,
      });

      const paymentUrl = response?.data?.paymentUrl;
      if (paymentUrl) {
        showToast('Đang chuyển hướng sang cổng thanh toán VNPAY...', 'info');
        window.location.href = paymentUrl;
      } else {
        showToast('Không tìm thấy đường dẫn thanh toán từ máy chủ.', 'error');
        setIsProcessing(false);
      }
    } catch (error: any) {
      const errorMsg = error?.response?.data?.message || 'Không thể tạo đơn đặt sân. Khung giờ này có thể đã được người khác đặt.';
      showToast(errorMsg, 'error');
      setIsProcessing(false);
    }
  };

  return (
    <div className="max-w-[1400px] mx-auto pt-8 px-4 pb-12">
      <button
        type="button"
        className="flex items-center gap-2 text-muted hover:text-primary transition-all mb-4 cursor-pointer"
        onClick={() => navigate(-1)}
        style={{ fontWeight: 500 }}
        aria-label="Quay lại trang chọn sân trước đó"
      >
        <ArrowLeft size={18} aria-hidden="true" />
        <span>Quay lại</span>
      </button>

      <CheckoutHeader />

      <main className="checkout-grid gap-6 md:gap-8">
        <CheckoutInvoice
          pitch={pitch}
          slot={slot}
          depositAmount={depositAmount}
          remainingAmount={remainingAmount}
          selectedDate={selectedDate}
          basePrice={basePrice}
        />
        <CheckoutForm
          depositAmount={depositAmount}
          isProcessing={isProcessing}
          onSubmit={handleBookingSubmit}
        />
      </main>
    </div>
  );
};

export default Checkout;
