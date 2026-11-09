import React, { useState, useEffect, useCallback } from 'react';
import { createPortal } from 'react-dom';
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { Download, X, Loader2, TrendingUp, TrendingDown, Calendar } from 'lucide-react';
import { adminReportService } from '../../services/adminReportService';
import type {
  DashboardSummary,
  RevenueTrendPoint,
  BookingFrequencyPoint,
  RevenueByPitchType,
} from '../../types/report';
import { showToast } from '../../utils/toast';

/**
 * =========================================================================================
 * SUB-COMPONENT: KpiCardSkeleton (Hiệu ứng Shimmer mượt mà cho 4 thẻ chỉ số)
 * =========================================================================================
 */
const KpiCardSkeleton: React.FC = () => (
  <div className="animate-pulse space-y-2 py-1">
    <div className="h-8 w-36 rounded bg-slate-200 dark:bg-slate-700/60" />
    <div className="h-4 w-44 rounded bg-slate-100 dark:bg-slate-800/60" />
  </div>
);

/**
 * =========================================================================================
 * SUB-COMPONENT: ChartLoadingSkeleton (Hiệu ứng Skeleton giả lập cột/đường khi đang tải)
 * =========================================================================================
 */
const ChartLoadingSkeleton: React.FC<{ message: string }> = ({ message }) => (
  <div className="h-full w-full flex flex-col items-center justify-center text-muted gap-3">
    <div className="flex items-center gap-2">
      <Loader2 size={24} className="animate-spin text-[var(--color-primary)]" />
      <span className="text-sm font-medium">{message}</span>
    </div>
    <div className="w-full max-w-md h-32 flex items-end gap-3 px-4 pt-4 opacity-30 animate-pulse">
      <div className="w-1/6 bg-slate-300 dark:bg-slate-700 h-1/3 rounded-t" />
      <div className="w-1/6 bg-slate-300 dark:bg-slate-700 h-2/3 rounded-t" />
      <div className="w-1/6 bg-slate-300 dark:bg-slate-700 h-full rounded-t" />
      <div className="w-1/6 bg-slate-300 dark:bg-slate-700 h-1/2 rounded-t" />
      <div className="w-1/6 bg-slate-300 dark:bg-slate-700 h-3/4 rounded-t" />
      <div className="w-1/6 bg-slate-300 dark:bg-slate-700 h-2/5 rounded-t" />
    </div>
  </div>
);

/**
 * =========================================================================================
 * COMPONENT CHÍNH: AdminDashboard (Màn hình Báo cáo Thống kê & Phân tích Doanh thu)
 * =========================================================================================
 * Hiện thực hóa Use Case UC018.1 trong SRS & Tối ưu theo FPMS-208:
 * 1. Khối bộ lọc thời gian đồng bộ kèm Quick Date Presets (Hôm nay, 7 ngày qua, Tháng này, Tháng trước).
 * 2. 4 Thẻ chỉ số KPI quan trọng với hiệu ứng Skeleton Shimmer mượt mà khi tải lại.
 * 3. 3 Biểu đồ phân tích trực quan Recharts chuẩn responsive (Zero CLS, không tràn khung ngang).
 * 4. Modal xác nhận xuất file Excel kèm trạng thái tiến trình isExporting & Spinner mượt mà.
 */
const AdminDashboard: React.FC = () => {
  // ---------------------------------------------------------------------------------------
  // 1. Quản lý trạng thái Thời gian lọc & Modal
  // ---------------------------------------------------------------------------------------
  const getDefaultDateRange = () => {
    const end = new Date();
    const start = new Date();
    start.setDate(end.getDate() - 6);
    return {
      start: start.toISOString().split('T')[0],
      end: end.toISOString().split('T')[0],
    };
  };

  const defaultDates = getDefaultDateRange();
  const [startDate, setStartDate] = useState<string>(defaultDates.start);
  const [endDate, setEndDate] = useState<string>(defaultDates.end);
  const [activePreset, setActivePreset] = useState<'today' | '7days' | 'thisMonth' | 'prevMonth' | null>('7days');

  /** Trạng thái hiển thị Modal xác nhận xuất file */
  const [showConfirmExport, setShowConfirmExport] = useState<boolean>(false);

  /** Trạng thái loading khi đang tải dữ liệu từ server */
  const [loading, setLoading] = useState<boolean>(false);

  /** Trạng thái tiến trình khi đang xuất file Excel (.xlsx) */
  const [isExporting, setIsExporting] = useState<boolean>(false);

  // ---------------------------------------------------------------------------------------
  // 2. Dữ liệu Báo cáo Thống kê từ Backend Spring Boot
  // ---------------------------------------------------------------------------------------
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [revenueTrend, setRevenueTrend] = useState<RevenueTrendPoint[]>([]);
  const [bookingFrequency, setBookingFrequency] = useState<BookingFrequencyPoint[]>([]);
  const [pitchTypeRevenue, setPitchTypeRevenue] = useState<RevenueByPitchType[]>([]);

  /** Bảng màu chuẩn nhận diện cho biểu đồ tròn */
  const PIE_COLORS = [
    'var(--color-primary, #047857)',
    'var(--color-secondary, #2563eb)',
    '#f59e0b',
    '#8b5cf6',
    '#ec4899',
  ];

  // ---------------------------------------------------------------------------------------
  // 3. Hàm gọi API tải dữ liệu đồng thời từ Backend
  // ---------------------------------------------------------------------------------------
  const fetchDashboardData = useCallback(async () => {
    if (!startDate || !endDate) {
      showToast('Vui lòng chọn đầy đủ ngày bắt đầu và kết thúc', 'warning');
      return;
    }

    if (new Date(startDate) > new Date(endDate)) {
      showToast('Ngày bắt đầu không được lớn hơn ngày kết thúc', 'warning');
      return;
    }

    try {
      setLoading(true);
      const params = { startDate, endDate };

      // Gọi đồng thời 4 API của AdminReportController để tối ưu thời gian tải trang
      const [summaryRes, trendRes, frequencyRes, pitchTypeRes] = await Promise.all([
        adminReportService.getDashboardSummary(params),
        adminReportService.getRevenueTrend(params),
        adminReportService.getBookingFrequency(params),
        adminReportService.getRevenueByPitchType(params),
      ]);

      setSummary(summaryRes);
      setRevenueTrend(trendRes);
      setBookingFrequency(frequencyRes);
      setPitchTypeRevenue(pitchTypeRes);
    } catch (error) {
      console.error('Lỗi khi tải dữ liệu báo cáo thống kê:', error);
    } finally {
      setLoading(false);
    }
  }, [startDate, endDate]);

  // Tự động tải dữ liệu lần đầu khi mở trang
  useEffect(() => {
    fetchDashboardData();
  }, [fetchDashboardData]);

  // ---------------------------------------------------------------------------------------
  // 4. Xử lý Quick Date Presets (Hôm nay, 7 ngày qua, Tháng này, Tháng trước)
  // ---------------------------------------------------------------------------------------
  const applyDatePreset = (preset: 'today' | '7days' | 'thisMonth' | 'prevMonth') => {
    setActivePreset(preset);
    const now = new Date();
    const pad = (n: number) => n.toString().padStart(2, '0');
    const toYMD = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

    let start = '';
    let end = toYMD(now);

    if (preset === 'today') {
      start = toYMD(now);
      end = toYMD(now);
    } else if (preset === '7days') {
      const s = new Date(now);
      s.setDate(now.getDate() - 6);
      start = toYMD(s);
    } else if (preset === 'thisMonth') {
      const s = new Date(now.getFullYear(), now.getMonth(), 1);
      start = toYMD(s);
    } else if (preset === 'prevMonth') {
      const s = new Date(now.getFullYear(), now.getMonth() - 1, 1);
      const e = new Date(now.getFullYear(), now.getMonth(), 0);
      start = toYMD(s);
      end = toYMD(e);
    }

    setStartDate(start);
    setEndDate(end);
  };

  // ---------------------------------------------------------------------------------------
  // 5. Định dạng hiển thị Tiền tệ, Số liệu và Tooltip
  // ---------------------------------------------------------------------------------------
  const formatCurrency = (val?: number): string => {
    if (val === undefined || val === null) return '0 đ';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
  };

  const formatYAxis = (tickItem: number) => {
    if (tickItem >= 1000000) {
      return (tickItem / 1000000).toFixed(1) + ' Tr';
    }
    if (tickItem >= 1000) {
      return (tickItem / 1000).toFixed(0) + ' K';
    }
    return tickItem.toString();
  };

  const CustomTooltip = ({ active, payload, label }: any) => {
    if (active && payload && payload.length) {
      return (
        <div
          style={{
            backgroundColor: 'var(--color-bg-elevated)',
            padding: '0.85rem 1rem',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-md)',
            boxShadow: 'var(--shadow-md)',
            minWidth: '180px',
          }}
        >
          <p className="font-bold text-sm mb-2 text-[var(--color-text-base)]">{label}</p>
          {payload.map((entry: any, index: number) => {
            const isRevenue = entry.dataKey === 'revenue' || entry.name === 'Doanh thu';
            const isPercentage =
              entry.unit === '%' ||
              entry.dataKey === 'percentage' ||
              entry.payload?.unit === '%' ||
              (entry.payload?.revenue !== undefined && entry.payload?.percentage !== undefined);

            let valueDisplay: string;
            if (isRevenue && typeof entry.value === 'number') {
              valueDisplay = formatCurrency(entry.value);
            } else if (isPercentage) {
              const revFormatted = entry.payload?.revenue ? ` (${formatCurrency(entry.payload.revenue)})` : '';
              valueDisplay = `${entry.value}%${revFormatted}`;
            } else {
              valueDisplay = `${entry.value} đơn`;
            }

            return (
              <p
                key={index}
                style={{
                  color: entry.color || 'var(--color-text-base)',
                  fontSize: '0.875rem',
                  fontWeight: 600,
                  margin: '4px 0',
                }}
              >
                {entry.name}: {valueDisplay}
              </p>
            );
          })}
        </div>
      );
    }
    return null;
  };

  // ---------------------------------------------------------------------------------------
  // 6. Xử lý Xuất file Báo cáo Excel kèm Spinner & Tiến trình
  // ---------------------------------------------------------------------------------------
  const handleExportExcelClick = () => {
    setShowConfirmExport(true);
  };

  const executeExportExcel = async () => {
    try {
      setIsExporting(true);
      const blob = await adminReportService.exportReportExcel({ startDate, endDate });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `BaoCao_DoanhThu_FPMS_${startDate}_${endDate}.xlsx`);
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
      window.URL.revokeObjectURL(url);
      showToast('Xuất báo cáo Excel thành công!', 'success');
      setShowConfirmExport(false);
    } catch (error: any) {
      if (error.response?.data instanceof Blob) {
        try {
          const text = await error.response.data.text();
          const errorJson = JSON.parse(text);
          showToast(errorJson.message || 'Không có dữ liệu để xuất báo cáo', 'warning');
        } catch {
          showToast('Xuất báo cáo thất bại. Vui lòng thử lại.', 'error');
        }
      } else {
        const msg = error.response?.data?.message || 'Xuất báo cáo thất bại. Vui lòng thử lại.';
        showToast(msg, 'error');
      }
    } finally {
      setIsExporting(false);
    }
  };

  // ---------------------------------------------------------------------------------------
  // 7. Modal Overlay sử dụng React Portal
  // ---------------------------------------------------------------------------------------
  const ModalOverlay = ({ children, onClose }: { children: React.ReactNode; onClose: () => void }) => {
    return createPortal(
      <div
        style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(0,0,0,0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 9999,
          padding: '1rem',
        }}
      >
        <div style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0 }} onClick={onClose} />
        <div
          className="card"
          style={{
            position: 'relative',
            zIndex: 10000,
            width: '100%',
            maxWidth: '500px',
            maxHeight: '90vh',
            overflowY: 'auto',
            display: 'flex',
            flexDirection: 'column',
          }}
        >
          {children}
        </div>
      </div>,
      document.body
    );
  };

  // ---------------------------------------------------------------------------------------
  // RENDER GIAO DIỆN CHÍNH
  // ---------------------------------------------------------------------------------------
  return (
    <div style={{ maxWidth: '1240px', margin: '0 auto', paddingBottom: '2.5rem' }}>
      {/* Tiêu đề trang */}
      <div style={{ marginBottom: '1.25rem' }}>
        <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--color-text-base)', margin: 0 }}>
          Báo Cáo & Thống Kê Doanh Thu
        </h1>
        <p style={{ fontSize: '0.875rem', color: 'var(--color-text-muted)', marginTop: '0.25rem', marginBottom: 0 }}>
          Theo dõi dòng tiền, tần suất đặt sân và tỷ trọng doanh thu toàn hệ thống
        </p>
      </div>

      {/* Thanh bộ lọc ngày & Nút xuất báo cáo (Responsive Grid/Flex) */}
      <div
        className="card mb-6"
        style={{
          padding: '1rem 1.25rem',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '1rem',
        }}
      >
        {/* Bên trái: Quick Presets & Hộp chọn ngày & Nút Lọc */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap', flex: '1 1 auto' }}>
          {/* Quick Date Presets */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', flexWrap: 'wrap' }}>
            {[
              { id: 'today', label: 'Hôm nay' },
              { id: '7days', label: '7 ngày qua' },
              { id: 'thisMonth', label: 'Tháng này' },
              { id: 'prevMonth', label: 'Tháng trước' },
            ].map((p) => {
              const isSelected = activePreset === p.id;
              return (
                <button
                  key={p.id}
                  type="button"
                  onClick={() => applyDatePreset(p.id as any)}
                  disabled={loading || isExporting}
                  style={{
                    padding: '0.4rem 0.75rem',
                    fontSize: '0.75rem',
                    fontWeight: 600,
                    borderRadius: 'var(--radius-md)',
                    border: isSelected ? '1px solid var(--color-primary, #047857)' : '1px solid var(--color-border)',
                    backgroundColor: isSelected ? 'var(--color-primary, #047857)' : 'var(--color-bg-base)',
                    color: isSelected ? '#ffffff' : 'var(--color-text-muted)',
                    cursor: loading || isExporting ? 'not-allowed' : 'pointer',
                    transition: 'all 0.15s ease',
                  }}
                >
                  {p.label}
                </button>
              );
            })}
          </div>

          {/* Hộp chọn khoảng ngày */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              height: '40px',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-md)',
              backgroundColor: 'var(--color-bg-base)',
              padding: '0 0.75rem',
            }}
          >
            <Calendar size={15} className="text-[var(--color-text-muted)] shrink-0" />
            <input
              type="date"
              value={startDate}
              onChange={(e) => {
                setStartDate(e.target.value);
                setActivePreset(null);
              }}
              onClick={(e) => {
                try {
                  (e.target as HTMLInputElement).showPicker();
                } catch {}
              }}
              style={{
                background: 'transparent',
                border: 'none',
                outline: 'none',
                color: 'var(--color-text-base)',
                fontFamily: 'inherit',
                cursor: 'pointer',
                fontSize: '0.85rem',
              }}
            />
            <span style={{ color: 'var(--color-text-muted)', fontWeight: 500 }}>-</span>
            <input
              type="date"
              value={endDate}
              onChange={(e) => {
                setEndDate(e.target.value);
                setActivePreset(null);
              }}
              onClick={(e) => {
                try {
                  (e.target as HTMLInputElement).showPicker();
                } catch {}
              }}
              style={{
                background: 'transparent',
                border: 'none',
                outline: 'none',
                color: 'var(--color-text-base)',
                fontFamily: 'inherit',
                cursor: 'pointer',
                fontSize: '0.85rem',
              }}
            />
          </div>

          {/* Nút bấm Lọc */}
          <button
            type="button"
            className="btn btn-secondary font-semibold"
            style={{
              height: '40px',
              padding: '0 1.25rem',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-md)',
              backgroundColor: 'var(--color-bg-surface)',
              color: 'var(--color-text-base)',
              fontSize: '0.875rem',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '0.5rem',
            }}
            onClick={fetchDashboardData}
            disabled={loading || isExporting}
          >
            {loading ? <Loader2 size={16} className="animate-spin text-[var(--color-primary)]" /> : 'Lọc'}
          </button>
        </div>

        {/* Bên phải: Nút Xuất báo cáo */}
        <button
          type="button"
          className="btn btn-primary font-semibold"
          style={{
            height: '40px',
            padding: '0 1.25rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '0.5rem',
            whiteSpace: 'nowrap',
            fontSize: '0.875rem',
            borderRadius: 'var(--radius-md)',
          }}
          onClick={handleExportExcelClick}
          disabled={loading || isExporting}
        >
          {isExporting ? (
            <>
              <Loader2 size={18} className="animate-spin" />
              <span>Đang xuất Excel...</span>
            </>
          ) : (
            <>
              <Download size={18} />
              <span>Xuất báo cáo</span>
            </>
          )}
        </button>
      </div>

      {/* 4 Thẻ KPI Dashboard (Responsive CSS Grid: 1 col on mobile, 2 on tablet, 4 on desktop) */}
      <div
        className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 sm:gap-6 mb-8"
      >
        {/* Thẻ 1: Doanh thu kỳ này */}
        <div className="card p-5" style={{ borderLeft: '4px solid var(--color-primary, #047857)' }}>
          <div className="text-muted text-xs font-bold tracking-wider uppercase mb-1">
            DOANH THU KỲ NÀY
          </div>
          {loading ? (
            <KpiCardSkeleton />
          ) : (
            <>
              <div className="text-3xl font-bold text-[var(--color-primary,#047857)]">
                {formatCurrency(summary?.currentRevenue)}
              </div>
              <div className="text-xs mt-2 flex items-center gap-1 font-semibold">
                {summary && (summary.revenueGrowthRate ?? 0) >= 0 ? (
                  <span className="text-[var(--color-success,#047857)] flex items-center gap-1">
                    <TrendingUp size={14} />
                    Tăng {summary.revenueGrowthRate}% so với kỳ trước
                  </span>
                ) : (
                  <span className="text-[var(--color-danger,#dc2626)] flex items-center gap-1">
                    <TrendingDown size={14} />
                    Giảm {Math.abs(summary?.revenueGrowthRate ?? 0)}% so với kỳ trước
                  </span>
                )}
              </div>
            </>
          )}
        </div>

        {/* Thẻ 2: Tổng số đơn */}
        <div className="card p-5" style={{ borderLeft: '4px solid var(--color-secondary, #2563eb)' }}>
          <div className="text-muted text-xs font-bold tracking-wider uppercase mb-1">
            TỔNG SỐ ĐƠN
          </div>
          {loading ? (
            <KpiCardSkeleton />
          ) : (
            <>
              <div className="text-3xl font-bold text-[var(--color-text-base)]">
                {`${summary?.totalBookings ?? 0} đơn`}
              </div>
              <div className="text-xs mt-2 text-muted">
                {summary?.completedBookings ?? 0} hoàn thành, {summary?.confirmedBookings ?? 0} đang đá/chờ
              </div>
            </>
          )}
        </div>

        {/* Thẻ 3: Tỉ lệ hủy đơn */}
        <div className="card p-5" style={{ borderLeft: '4px solid var(--color-danger, #dc2626)' }}>
          <div className="text-muted text-xs font-bold tracking-wider uppercase mb-1">
            TỈ LỆ HỦY ĐƠN
          </div>
          {loading ? (
            <KpiCardSkeleton />
          ) : (
            <>
              <div className="text-3xl font-bold text-[var(--color-danger,#dc2626)]">
                {`${summary?.cancellationRate ?? 0}%`}
              </div>
              <div className="text-xs mt-2 text-muted">
                {summary && (summary.cancellationRateDiff ?? 0) <= 0
                  ? `Giảm ${Math.abs(summary.cancellationRateDiff)}% so với kỳ trước`
                  : `Tăng ${summary?.cancellationRateDiff ?? 0}% so với kỳ trước`}
              </div>
            </>
          )}
        </div>

        {/* Thẻ 4: Lượt khách mới */}
        <div className="card p-5" style={{ borderLeft: '4px solid var(--color-warning, #b45309)' }}>
          <div className="text-muted text-xs font-bold tracking-wider uppercase mb-1">
            LƯỢT KHÁCH MỚI
          </div>
          {loading ? (
            <KpiCardSkeleton />
          ) : (
            <>
              <div className="text-3xl font-bold text-[var(--color-text-base)]">
                {`${summary?.newCustomersCount ?? 0} người`}
              </div>
              <div className="text-xs mt-2 text-muted">
                Tỉ lệ khách quay lại: {summary?.repeatCustomerRate ?? 0}%
              </div>
            </>
          )}
        </div>
      </div>

      {/* Khu vực 3 Biểu đồ phân tích (Responsive CSS Grid & Zero Overflow) */}
      <div className="flex flex-col gap-6 mb-6">
        {/* 1. Biểu đồ đường Doanh thu theo ngày */}
        <div className="card p-4 sm:p-5 w-full min-w-0 overflow-hidden relative">
          <div className="flex justify-between items-center mb-6">
            <div>
              <h2 className="text-lg font-bold text-[var(--color-text-base)]">Biểu đồ Doanh thu</h2>
              <p className="text-xs text-muted">Doanh thu phát sinh theo từng ngày trong khoảng thời gian đã chọn</p>
            </div>
          </div>
          <div style={{ width: '100%', height: 350, position: 'relative' }}>
            {/* Lớp phủ loading khi đang fetch lại dữ liệu */}
            {loading && revenueTrend.length > 0 && (
              <div
                style={{
                  position: 'absolute',
                  inset: 0,
                  zIndex: 10,
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '0.5rem',
                  backgroundColor: 'rgba(255, 255, 255, 0.6)',
                  backdropFilter: 'blur(1.5px)',
                  borderRadius: 'var(--radius-md)',
                }}
              >
                <Loader2 size={26} className="animate-spin text-[var(--color-primary)]" />
                <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--color-text-muted)' }}>
                  Đang cập nhật biểu đồ doanh thu...
                </span>
              </div>
            )}

            {loading && revenueTrend.length === 0 ? (
              <ChartLoadingSkeleton message="Đang kết xuất biểu đồ doanh thu..." />
            ) : revenueTrend.length === 0 ? (
              <div className="h-full flex items-center justify-center text-muted">
                Không có dữ liệu doanh thu trong khoảng thời gian này
              </div>
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <LineChart
                  data={revenueTrend.map((item) => ({
                    ...item,
                    name: `${item.dayName} (${item.date.split('-').slice(1).join('/')})`,
                  }))}
                  margin={{ top: 10, right: 30, left: 10, bottom: 0 }}
                >
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border)" vertical={false} />
                  <XAxis dataKey="name" stroke="var(--color-text-muted)" fontSize={12} tickMargin={10} />
                  <YAxis
                    stroke="var(--color-text-muted)"
                    fontSize={12}
                    tickFormatter={formatYAxis}
                    width={65}
                  />
                  <Tooltip content={<CustomTooltip />} />
                  <Legend iconType="circle" wrapperStyle={{ paddingTop: '20px' }} />
                  <Line
                    type="monotone"
                    dataKey="revenue"
                    name="Doanh thu"
                    stroke="var(--color-primary, #047857)"
                    strokeWidth={3}
                    activeDot={{ r: 8, strokeWidth: 0 }}
                    dot={{ r: 4, strokeWidth: 2, fill: 'var(--color-bg-surface)' }}
                  />
                </LineChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>

        {/* 2 & 3. Biểu đồ Tần suất đặt sân & Tỷ trọng loại sân nằm cạnh nhau (Grid chuẩn 1 col mobile, 2 col desktop) */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 min-w-0">
          {/* Biểu đồ cột Tần suất đặt sân theo thứ trong tuần */}
          <div className="card p-4 sm:p-5 min-w-0 overflow-hidden relative">
            <h2 className="text-lg font-bold text-[var(--color-text-base)] mb-1">Tần suất Đặt sân</h2>
            <p className="text-xs text-muted mb-6">Số lượt đặt sân phân bổ theo các thứ trong tuần (T2 - CN)</p>
            <div style={{ width: '100%', height: 350, position: 'relative' }}>
              {/* Lớp phủ loading khi đang fetch lại dữ liệu */}
              {loading && bookingFrequency.length > 0 && (
                <div
                  style={{
                    position: 'absolute',
                    inset: 0,
                    zIndex: 10,
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '0.5rem',
                    backgroundColor: 'rgba(255, 255, 255, 0.6)',
                    backdropFilter: 'blur(1.5px)',
                    borderRadius: 'var(--radius-md)',
                  }}
                >
                  <Loader2 size={26} className="animate-spin text-[var(--color-secondary)]" />
                  <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--color-text-muted)' }}>
                    Đang cập nhật tần suất đơn...
                  </span>
                </div>
              )}

              {loading && bookingFrequency.length === 0 ? (
                <ChartLoadingSkeleton message="Đang tải tần suất đơn..." />
              ) : bookingFrequency.length === 0 ? (
                <div className="h-full flex items-center justify-center text-muted">
                  Không có dữ liệu tần suất đặt sân
                </div>
              ) : (
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart
                    data={bookingFrequency.map((item) => ({
                      ...item,
                      name: item.dayOfWeekName,
                      bookings: item.bookingCount,
                    }))}
                    margin={{ top: 10, right: 10, left: -15, bottom: 0 }}
                  >
                    <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border)" vertical={false} />
                    <XAxis dataKey="name" stroke="var(--color-text-muted)" fontSize={12} tickMargin={10} />
                    <YAxis stroke="var(--color-text-muted)" fontSize={12} />
                    <Tooltip content={<CustomTooltip />} cursor={{ fill: 'rgba(0,0,0,0.04)' }} />
                    <Legend iconType="circle" wrapperStyle={{ paddingTop: '20px' }} />
                    <Bar
                      dataKey="bookings"
                      name="Số đơn đặt"
                      fill="var(--color-secondary, #2563eb)"
                      radius={[4, 4, 0, 0]}
                      barSize={36}
                    />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </div>
          </div>

          {/* Biểu đồ tròn Cơ cấu doanh thu theo Loại sân */}
          <div className="card p-4 sm:p-5 min-w-0 overflow-hidden relative">
            <h2 className="text-lg font-bold text-[var(--color-text-base)] mb-1">Doanh thu theo Loại sân</h2>
            <p className="text-xs text-muted mb-6">Tỷ trọng đóng góp doanh thu giữa Sân 5 người và Sân 7 người</p>
            <div style={{ width: '100%', height: 350, position: 'relative' }}>
              {/* Lớp phủ loading khi đang fetch lại dữ liệu */}
              {loading && pitchTypeRevenue.length > 0 && (
                <div
                  style={{
                    position: 'absolute',
                    inset: 0,
                    zIndex: 10,
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '0.5rem',
                    backgroundColor: 'rgba(255, 255, 255, 0.6)',
                    backdropFilter: 'blur(1.5px)',
                    borderRadius: 'var(--radius-md)',
                  }}
                >
                  <Loader2 size={26} className="animate-spin text-[var(--color-primary)]" />
                  <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--color-text-muted)' }}>
                    Đang tính toán cơ cấu sân...
                  </span>
                </div>
              )}

              {loading && pitchTypeRevenue.length === 0 ? (
                <ChartLoadingSkeleton message="Đang tính toán cơ cấu sân..." />
              ) : pitchTypeRevenue.length === 0 ? (
                <div className="h-full flex items-center justify-center text-muted">
                  Chưa có số liệu doanh thu theo loại sân
                </div>
              ) : (
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={pitchTypeRevenue.map((item) => ({
                        name: item.pitchTypeName,
                        value: item.percentage,
                        revenue: item.revenue,
                        bookingCount: item.bookingCount,
                        unit: '%',
                      }))}
                      cx="50%"
                      cy="48%"
                      innerRadius={75}
                      outerRadius={115}
                      paddingAngle={5}
                      dataKey="value"
                      label={({ name, percent }: { name?: string; percent?: number }) =>
                        `${name || ''}: ${(((percent ?? 0) * 100)).toFixed(0)}%`
                      }
                      labelLine={false}
                    >
                      {pitchTypeRevenue.map((_, index) => (
                        <Cell key={`cell-${index}`} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip content={<CustomTooltip />} />
                    <Legend
                      iconType="circle"
                      layout="horizontal"
                      verticalAlign="bottom"
                      align="center"
                      wrapperStyle={{ paddingTop: '20px' }}
                    />
                  </PieChart>
                </ResponsiveContainer>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Modal Xác Nhận Xuất Báo Cáo (React Portal) */}
      {showConfirmExport && (
        <ModalOverlay onClose={() => !isExporting && setShowConfirmExport(false)}>
          <div className="flex justify-between items-center mb-4" style={{ flexShrink: 0 }}>
            <h2 className="text-xl font-bold text-[var(--color-text-base)]">Xác Nhận Xuất File Báo Cáo</h2>
            <button
              onClick={() => !isExporting && setShowConfirmExport(false)}
              className="text-muted hover:text-[var(--color-text-base)] p-1 transition-colors"
              disabled={isExporting}
            >
              <X size={20} />
            </button>
          </div>
          <div className="mb-6">
            <p className="text-sm text-[var(--color-text-base)] leading-relaxed">
              Bạn có chắc chắn muốn xuất báo cáo doanh thu từ ngày{' '}
              <strong className="text-[var(--color-primary)]">{startDate}</strong> đến ngày{' '}
              <strong className="text-[var(--color-primary)]">{endDate}</strong> không?
            </p>
            <p className="text-muted text-xs mt-3 p-3 bg-[var(--color-bg-base)] rounded">
              File báo cáo định dạng <strong>.xlsx</strong> sẽ bao gồm: Báo cáo tổng hợp KPI và Danh sách
              chi tiết các đơn đặt sân tương ứng theo kỳ lọc.
            </p>
          </div>
          <div
            style={{
              flexShrink: 0,
              paddingTop: '1rem',
              marginTop: 'auto',
              borderTop: '1px solid var(--color-border)',
              display: 'flex',
              gap: '1rem',
            }}
          >
            <button
              className="btn btn-secondary w-1/2"
              onClick={() => setShowConfirmExport(false)}
              disabled={isExporting}
            >
              Hủy bỏ
            </button>
            <button
              className="btn btn-primary w-1/2 flex items-center justify-center gap-2"
              onClick={executeExportExcel}
              disabled={isExporting}
            >
              {isExporting ? (
                <>
                  <Loader2 size={16} className="animate-spin" />
                  <span>Đang tạo file...</span>
                </>
              ) : (
                <span>Xác nhận xuất</span>
              )}
            </button>
          </div>
        </ModalOverlay>
      )}
    </div>
  );
};

export default AdminDashboard;
