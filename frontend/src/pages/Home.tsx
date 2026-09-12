import React, { useState } from 'react';
import { Calendar, CreditCard, Smartphone, Search, Star } from 'lucide-react';
import { useNavigate } from 'react-router-dom';


const Home: React.FC = () => {
  const navigate = useNavigate();
  const [pitchType, setPitchType] = useState('all');
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);

  const handleSearch = () => {
    navigate(`/book-pitch?type=${pitchType}&date=${date}`);
  };

  return (
    <div className="animate-fade-in" style={{ marginTop: '-2rem' }}>
      {/* 1. Hero Section */}
      <section className="hero-section" style={{ minHeight: '65vh', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', marginBottom: '4rem', background: 'linear-gradient(135deg, rgba(5,150,105,0.9) 0%, rgba(16,185,129,0.85) 50%, rgba(6,182,212,0.9) 100%), url("https://images.unsplash.com/photo-1518605368461-1ee7e53f0b2f?q=80&w=2070&auto=format&fit=crop") center/cover no-repeat' }}>
        <div className="container">
          <div className="inline-block mb-6 px-4 py-1.5 rounded-full" style={{ backgroundColor: 'rgba(255, 255, 255, 0.15)', border: '1px solid rgba(255, 255, 255, 0.2)', fontSize: '0.875rem' }}>
            ⚡ Đặt sân — Thanh toán — Xác nhận đơn chỉ trong 2 phút
          </div>

          <h1 className="text-4xl md:text-6xl font-bold mb-6 mt-4 md:mt-0 px-2" style={{ lineHeight: 1.2 }}>
            Đặt sân bóng đá<br className="hidden md:block" /> trực tuyến dễ dàng
          </h1>

          <p className="text-lg md:text-xl opacity-90 mb-12 max-w-2xl mx-auto">
            Tìm sân trống, chọn giờ, thanh toán tiền cọc online và nhận thông báo xác nhận ngay tức thì.
          </p>

          <div className="home-search-bar transition-all">
            {/* Pitch Type */}
            <div
              className="home-search-field"
              onClick={() => {
                const select = document.getElementById('home-pitch-type') as HTMLSelectElement;
                if (select) select.focus();
              }}
            >
              <label className="text-sm font-medium text-muted whitespace-nowrap" style={{ cursor: 'pointer' }}>Loại sân:</label>
              <select
                id="home-pitch-type"
                value={pitchType}
                onChange={e => setPitchType(e.target.value)}
                className="home-search-input"
              >
                <option value="all">Tìm tất cả các sân</option>
                <option value="5">Sân 5 người</option>
                <option value="7">Sân 7 người</option>
              </select>
            </div>

            {/* Date */}
            <div
              className="home-search-field"
              onClick={() => {
                const input = document.getElementById('home-date-picker') as HTMLInputElement;
                if (input && 'showPicker' in HTMLInputElement.prototype) {
                  try { input.showPicker(); } catch (e) { }
                }
              }}
            >
              <label className="text-sm font-medium text-muted whitespace-nowrap" style={{ cursor: 'pointer' }}>Ngày đá:</label>
              <input
                id="home-date-picker"
                type="date"
                value={date}
                onChange={e => setDate(e.target.value)}
                className="home-search-input"
                onClick={(e) => {
                  if ('showPicker' in HTMLInputElement.prototype) {
                    try { (e.target as HTMLInputElement).showPicker(); } catch (err) { }
                  }
                }}
              />
            </div>

            {/* Submit Button */}
            <button
              className="btn btn-primary home-search-btn transition-all"
              onClick={handleSearch}
            >
              <Search size={20} className="mr-2" /> Tìm sân
            </button>
          </div>
        </div>
      </section>

      <div className="container">
        {/* Stats */}
        <div className="flex justify-center items-center gap-4 md:gap-12 text-center mb-16 px-2">
          <div className="flex-1">
            <div className="text-2xl md:text-3xl font-bold" style={{ color: 'var(--color-primary)' }}>24+</div>
            <div className="text-xs md:text-sm text-muted font-medium">Sân bóng</div>
          </div>
          <div className="flex-1">
            <div className="text-2xl md:text-3xl font-bold" style={{ color: 'var(--color-primary)' }}>2.8K+</div>
            <div className="text-xs md:text-sm text-muted font-medium">Lượt đặt</div>
          </div>
          <div className="flex-1">
            <div className="text-2xl md:text-3xl font-bold flex items-center justify-center gap-1" style={{ color: 'var(--color-primary)' }}>
              4.9<Star size={18} className="fill-warning text-warning md:w-6 md:h-6" />
            </div>
            <div className="text-xs md:text-sm text-muted font-medium">Đánh giá</div>
          </div>
        </div>
      </div>

      {/* 2. Features Section */}
      <section className="container mb-24">
        <div className="text-center mb-12">
          <h2 className="text-3xl font-bold mb-4">Tại sao chọn Soccer365?</h2>
          <p className="text-muted">Giải pháp đặt sân bóng số 1, được hàng nghìn team tin dùng</p>
        </div>

        <div className="features-grid">
          <div className="feature-card">
            <div className="feature-icon-wrapper">
              <Calendar size={48} />
            </div>
            <h3 className="text-xl font-bold mb-3">Đặt lịch trực quan</h3>
            <p className="text-sm text-muted leading-relaxed">
              Chọn sân bằng bảng trực quan theo thời gian. Thấy ngay slot nào còn trống, click là đặt.
            </p>
          </div>

          <div className="feature-card highlight" style={{ backgroundColor: 'var(--color-primary-light)', borderColor: 'var(--color-primary)' }}>
            <div className="feature-icon-wrapper">
              <CreditCard size={48} />
            </div>
            <h3 className="text-xl font-bold mb-3" style={{ color: 'var(--color-primary-hover)' }}>Thanh toán VNPAY</h3>
            <p className="text-sm text-muted leading-relaxed">
              Đặt cọc 30% qua VNPAY QR an toàn. Xác nhận tức thì, nhận vé ngay mà không cần chờ đợi.
            </p>
          </div>

          <div className="feature-card">
            <div className="feature-icon-wrapper">
              <Smartphone size={48} />
            </div>
            <h3 className="text-xl font-bold mb-3">Truy cập mọi lúc mọi nơi</h3>
            <p className="text-sm text-muted leading-relaxed">
              Hỗ trợ đa nền tảng. Dễ dàng đặt sân, theo dõi đơn và thanh toán tiện lợi ngay trên điện thoại.
            </p>
          </div>
        </div>
      </section>
    </div>
  );
};

export default Home;
