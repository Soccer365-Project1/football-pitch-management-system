import React, { useState } from 'react';
import { Calendar, Smartphone, Search, Star } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { usePitchTypes } from '../hooks/queries/usePitchQueries.ts';
import { useDocumentTitle } from '../hooks/useDocumentTitle.ts';

const Home: React.FC = () => {
  const navigate = useNavigate();
  const [pitchType, setPitchType] = useState('all');
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);
  const { data: pitchTypes = [] } = usePitchTypes();

  useDocumentTitle(
    'Soccer365 - Đặt Sân Bóng Đá Trực Tuyến Nhanh Chóng & Dễ Dàng',
    'Hệ thống đặt sân bóng đá Soccer365. Tra cứu lịch sân trống trực quan theo thời gian, thanh toán tiền cọc online an toàn qua VNPAY, xác nhận tức thì.'
  );

  const handleSearch = () => {
    navigate(`/book-pitch?date=${date}&type=${pitchType}`);
  };

  return (
    <div className="animate-fade-in" style={{ marginTop: '-2rem' }}>
      {/* 1. Hero Section */}
      <section
        className="hero-section"
        style={{
          minHeight: '65vh',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          marginBottom: '4rem',
          background: 'linear-gradient(135deg, rgba(5,150,105,0.92) 0%, rgba(16,185,129,0.88) 50%, rgba(6,182,212,0.92) 100%), url("https://images.unsplash.com/photo-1518605368461-1ee7e53f0b2f?q=80&w=2070&auto=format&fit=crop") center/cover no-repeat',
        }}
      >
        <div className="container flex flex-col items-center">
          <div
            className="inline-block mb-6 px-4 py-1.5 rounded-full font-medium"
            style={{
              backgroundColor: 'rgba(255, 255, 255, 0.18)',
              border: '1px solid rgba(255, 255, 255, 0.3)',
              fontSize: '0.875rem',
            }}
          >
            ⚡ Đặt sân — Thanh toán — Xác nhận đơn chỉ trong 2 phút
          </div>

          <h1 className="text-4xl md:text-6xl font-bold mb-6 mt-4 md:mt-0 px-2 text-center text-white" style={{ lineHeight: 1.2 }}>
            Đặt sân bóng đá<br className="hidden md:block" /> trực tuyến dễ dàng
          </h1>

          <p className="text-lg md:text-xl text-white opacity-95 mb-12 max-w-2xl mx-auto text-center font-normal">
            Tìm sân trống, chọn giờ, thanh toán tiền cọc online và nhận thông báo xác nhận ngay tức thì.
          </p>

          {/* Search Bar */}
          <div className="home-search-bar transition-all" role="search" aria-label="Tìm kiếm lịch sân bóng">
            {/* Pitch Type */}
            <div
              className="home-search-field"
              onClick={() => {
                const select = document.getElementById('home-pitch-type') as HTMLSelectElement;
                if (select) select.focus();
              }}
            >
              <label htmlFor="home-pitch-type" className="text-sm font-medium text-muted whitespace-nowrap" style={{ cursor: 'pointer' }}>
                Loại sân:
              </label>
              <select
                id="home-pitch-type"
                aria-label="Chọn loại sân bóng để tìm kiếm"
                value={pitchType}
                onChange={(e) => setPitchType(e.target.value)}
                className="home-search-input"
              >
                <option value="all">Tìm tất cả các sân</option>
                {pitchTypes.map((type) => (
                  <option key={type.id} value={type.id.toString()}>
                    {type.name}
                  </option>
                ))}
              </select>
            </div>

            {/* Date */}
            <div
              className="home-search-field"
              onClick={() => {
                const input = document.getElementById('home-date-picker') as HTMLInputElement;
                if (input && 'showPicker' in HTMLInputElement.prototype) {
                  try {
                    input.showPicker();
                  } catch (e) { }
                }
              }}
            >
              <label htmlFor="home-date-picker" className="text-sm font-medium text-muted whitespace-nowrap" style={{ cursor: 'pointer' }}>
                Ngày đá:
              </label>
              <input
                id="home-date-picker"
                aria-label="Chọn ngày đá để tìm kiếm sân trống"
                type="date"
                min={new Date(new Date().getTime() - new Date().getTimezoneOffset() * 60000).toISOString().split('T')[0]}
                value={date}
                onChange={(e) => setDate(e.target.value)}
                className="home-search-input"
                onClick={(e) => {
                  if ('showPicker' in HTMLInputElement.prototype) {
                    try {
                      (e.target as HTMLInputElement).showPicker();
                    } catch (err) { }
                  }
                }}
              />
            </div>

            {/* Submit Button */}
            <button
              className="btn btn-primary home-search-btn transition-all"
              onClick={handleSearch}
              aria-label="Tra cứu lịch sân bóng trống"
            >
              <Search size={20} className="mr-2" aria-hidden="true" />
              <span>Tìm sân</span>
            </button>
          </div>
        </div>
      </section>

      {/* Stats Section */}
      <section className="container" aria-label="Thống kê hệ thống">
        <div className="flex justify-center items-center gap-4 md:gap-12 text-center mb-16 px-2">
          <div className="flex-1">
            <div className="text-2xl md:text-3xl font-bold" style={{ color: 'var(--color-primary)' }}>
              24+
            </div>
            <div className="text-xs md:text-sm text-muted font-medium">Sân bóng hiện đại</div>
          </div>
          <div className="flex-1">
            <div className="text-2xl md:text-3xl font-bold" style={{ color: 'var(--color-primary)' }}>
              2.8K+
            </div>
            <div className="text-xs md:text-sm text-muted font-medium">Lượt đặt thành công</div>
          </div>
          <div className="flex-1">
            <div className="text-2xl md:text-3xl font-bold flex items-center justify-center gap-1" style={{ color: 'var(--color-primary)' }}>
              4.9
              <Star size={18} className="fill-warning text-warning md:w-6 md:h-6" aria-hidden="true" />
            </div>
            <div className="text-xs md:text-sm text-muted font-medium">Đánh giá 5 sao</div>
          </div>
        </div>
      </section>

      {/* 2. Features Section */}
      <section className="container mb-24" aria-label="Ưu điểm vượt trội của Soccer365">
        <div className="text-center mb-12">
          <h2 className="text-3xl font-bold mb-4">Tại sao chọn Soccer365?</h2>
          <p className="text-muted">Giải pháp đặt sân bóng đá số 1, được hàng nghìn đội bóng tin dùng</p>
        </div>

        <div className="features-grid">
          <article className="feature-card">
            <div className="feature-icon-wrapper" aria-hidden="true">
              <Calendar size={42} />
            </div>
            <h3 className="text-xl font-bold mb-3">Đặt lịch trực quan</h3>
            <p className="text-sm text-muted leading-relaxed">
              Chọn sân bằng bảng trực quan theo thời gian thực. Thấy ngay khung giờ nào còn trống, chọn là đặt ngay.
            </p>
          </article>

          <article
            className="feature-card highlight"
            style={{
              backgroundColor: 'var(--color-primary-light)',
              borderColor: 'var(--color-primary)',
            }}
          >
            <div className="feature-icon-wrapper flex items-center justify-center mb-3">
              <img
                src="https://sandbox.vnpayment.vn/paymentv2/Images/brands/logo.svg"
                alt="Logo cổng thanh toán điện tử quốc gia VNPAY-QR"
                width="110"
                height="32"
                loading="lazy"
                style={{ height: '32px', width: 'auto', aspectRatio: '110/32', objectFit: 'contain' }}
              />
            </div>
            <h3 className="text-xl font-bold mb-3" style={{ color: 'var(--color-primary-hover)' }}>
              Thanh toán VNPAY-QR
            </h3>
            <p className="text-sm text-muted leading-relaxed">
              Đặt cọc 30% qua cổng VNPAY-QR an toàn, tiện lợi. Xác nhận đơn tức thì mà không cần chờ đợi xác thực thủ công.
            </p>
          </article>

          <article className="feature-card">
            <div className="feature-icon-wrapper" aria-hidden="true">
              <Smartphone size={42} />
            </div>
            <h3 className="text-xl font-bold mb-3">Tiện lợi trên mọi thiết bị</h3>
            <p className="text-sm text-muted leading-relaxed">
              Giao diện tối ưu hoàn hảo cho cả máy tính và điện thoại. Dễ dàng theo dõi lịch đấu và quản lý đơn đặt mọi lúc.
            </p>
          </article>
        </div>
      </section>
    </div>
  );
};

export default Home;
