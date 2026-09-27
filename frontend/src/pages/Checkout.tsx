import React, { useState } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { usePitches, useTimeSlots, useScheduleGrid } from '../hooks/queries/usePitchQueries.ts';

import PaymentSuccess from '../components/checkout/PaymentSuccess.tsx';
import PaymentFailed from '../components/checkout/PaymentFailed.tsx';
import CheckoutHeader from '../components/checkout/CheckoutHeader.tsx';
import CheckoutInvoice from '../components/checkout/CheckoutInvoice.tsx';
import CheckoutForm from '../components/checkout/CheckoutForm.tsx';

const Checkout: React.FC = () => {
  const { pitchId, timeSlotId } = useParams<{ pitchId: string, timeSlotId: string }>();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const selectedDate = searchParams.get('date') || new Date().toISOString().split('T')[0];

  const { data: pitches = [], isLoading: isLoadingPitches } = usePitches('all');
  const { data: timeSlots = [], isLoading: isLoadingSlots } = useTimeSlots();
  const { data: scheduleData = { bookings: [], prices: [] }, isLoading: isLoadingSchedule } = useScheduleGrid(selectedDate, 'all');

  const [paymentStatus, setPaymentStatus] = useState<'IDLE' | 'SUCCESS' | 'FAILED'>(() => {
    const statusParam = searchParams.get('status');
    if (statusParam === 'failed' || statusParam === 'error') return 'FAILED';
    if (statusParam === 'success') return 'SUCCESS';
    return 'IDLE';
  });
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

  const handlePaymentSuccess = () => {
    setIsProcessing(true);
    setTimeout(() => {
      setIsProcessing(false);
      setPaymentStatus('SUCCESS');
    }, 1200);
  };

  const handlePaymentFailed = () => {
    setIsProcessing(true);
    setTimeout(() => {
      setIsProcessing(false);
      setPaymentStatus('FAILED');
    }, 1200);
  };

  if (paymentStatus === 'SUCCESS') {
    return (
      <PaymentSuccess
        pitch={pitch}
        slot={slot}
        depositAmount={depositAmount}
        remainingAmount={remainingAmount}
        onViewBookings={() => navigate('/my-bookings')}
        onBookMore={() => navigate('/book-pitch')}
      />
    );
  }

  if (paymentStatus === 'FAILED') {
    return (
      <PaymentFailed
        pitch={pitch}
        slot={slot}
        depositAmount={depositAmount}
        onRetry={() => setPaymentStatus('IDLE')}
        onBookMore={() => navigate('/')}
      />
    );
  }

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
          onPaymentSuccess={handlePaymentSuccess}
          onPaymentFailed={handlePaymentFailed}
        />
      </div>
    </div>
  );
};

export default Checkout;
