import React, { useState } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { usePitches, useTimeSlots, useScheduleGrid } from '../hooks/queries/usePitchQueries.ts';

import CheckoutHeader from '../components/checkout/CheckoutHeader.tsx';
import CheckoutInvoice from '../components/checkout/CheckoutInvoice.tsx';
import CheckoutForm from '../components/checkout/CheckoutForm.tsx';
import { bookingService } from '../services/bookingService.ts';
import { showToast } from '../utils/toast.ts';

const Checkout: React.FC = () => {
  const { pitchId, timeSlotId } = useParams<{ pitchId: string, timeSlotId: string }>();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const selectedDate = searchParams.get('date') || new Date().toISOString().split('T')[0];

  const { data: pitches = [], isLoading: isLoadingPitches } = usePitches('all');
  const { data: timeSlots = [], isLoading: isLoadingSlots } = useTimeSlots();
  const { data: scheduleData = { bookings: [], prices: [] }, isLoading: isLoadingSchedule } = useScheduleGrid(selectedDate, 'all');

  const [isProcessing, setIsProcessing] = useState(false);

  if (isLoadingPitches || isLoadingSlots || isLoadingSchedule) {
    return (
      <div className="flex justify-center items-center py-12">
        <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
      </div>
    );
  }

  const pitch = pitches.find(p => p.id === Number(pitchId));
  const slot = timeSlots.find(t => t.id === Number(timeSlotId));

  if (!pitch || !slot) {
    return <div className="text-center mt-8 text-danger">Không tìm thấy thông tin sân hoặc khung giờ!</div>;
  }

  const pitchPriceItem = (scheduleData.prices || []).find(
    (p: any) => p.pitchTypeId === pitch.pitchType?.id && p.isPeakHour === slot.isPeakHour
  );
  const basePrice = pitchPriceItem ? pitchPriceItem.price : (slot.isPeakHour ? 300000 : 250000);
  
  const depositRatio = 0.3; // 30% cọc
  const depositAmount = basePrice * depositRatio;
  const remainingAmount = basePrice - depositAmount;

  const handleBookingSubmit = async (formData: { guestName: string; guestPhone: string; customerNote: string }) => {
    setIsProcessing(true);
    try {
      await bookingService.createBooking({
        pitchId: Number(pitchId),
        timeSlotId: Number(timeSlotId),
        bookingDate: selectedDate,
        ...formData
      });
      showToast('Giữ chỗ thành công! Vui lòng thanh toán cọc trong vòng 10 phút.', 'success');
      navigate('/my-bookings');
    } catch (error: any) {
      console.error('Lỗi khi đặt sân:', error);
      showToast(error.response?.data?.message || 'Có lỗi xảy ra khi đặt sân. Vui lòng thử lại.', 'error');
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="max-w-[1400px] mx-auto pt-8 px-4 pb-12">
      <button
        className="flex items-center gap-2 text-muted hover:text-primary transition-all mb-4"
        onClick={() => navigate(-1)}
        style={{ fontWeight: 500 }}
      >
        <ArrowLeft size={18} /> Quay lại
      </button>

      <CheckoutHeader />

      <div className="checkout-grid gap-6 md:gap-8">
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
      </div>
    </div>
  );
};

export default Checkout;
