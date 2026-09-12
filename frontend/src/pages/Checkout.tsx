import React from 'react';
import { Link, useParams } from 'react-router-dom';

const Checkout: React.FC = () => {
  const { timeSlotId, pitchId } = useParams<{ timeSlotId: string; pitchId: string }>();

  return (
    <div className="container mx-auto px-4 py-16 text-center animate-fade-in">
      <h1 className="text-3xl font-bold mb-4">Thanh toán cọc</h1>
      <p className="text-muted text-lg mb-8">Tính năng của trang Thanh toán cọc đang phát triển...</p>
      <Link to="/" className="btn btn-primary inline-flex items-center">
        Quay lại trang chủ
      </Link>
    </div>
  );
};

export default Checkout;
