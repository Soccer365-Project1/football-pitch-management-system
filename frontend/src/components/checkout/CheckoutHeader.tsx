import React from 'react';
import { CheckCircle } from 'lucide-react';

const CheckoutHeader: React.FC = () => (
  <div
    className="mb-8 shadow-lg relative overflow-hidden"
    style={{
      borderRadius: '1rem',
      background: 'linear-gradient(135deg, rgba(5,150,105,0.9) 0%, rgba(16,185,129,0.85) 50%, rgba(6,182,212,0.9) 100%), url("https://images.unsplash.com/photo-1518605368461-1ee7e53f0b2f?q=80&w=2070&auto=format&fit=crop") center/cover no-repeat',
      color: 'white',
      padding: '3rem 2rem',
      textAlign: 'center',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center'
    }}
  >
    <h1 className="text-2xl md:text-4xl font-bold mb-3 flex items-center justify-center gap-2 md:gap-3 text-white">
      <CheckCircle size={32} />
      <span>Xác nhận đặt sân</span>
    </h1>
    <p className="text-base md:text-lg" style={{ color: 'rgba(255, 255, 255, 0.9)' }}>Vui lòng kiểm tra kỹ thông tin đơn đặt và tiến hành đặt cọc giữ sân.</p>
  </div>
);

export default CheckoutHeader;
