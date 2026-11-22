-- =========================================================================================
-- FPMS-208: ĐÁNH COMPOSITE INDEX PHỤC VỤ TỐI ƯU HÓA TRUY VẤN VẬN HÀNH & BÁO CÁO DOANH THU
-- =========================================================================================

-- 1. Composite Index phục vụ thống kê doanh thu và lọc danh sách đơn theo ngày & trạng thái
CREATE INDEX IF NOT EXISTS idx_bookings_date_status
    ON bookings (booking_date, status, is_deleted);

-- 2. Composite Index phục vụ thống kê tỷ lệ khách hàng quay lại (Repeat Customers)
CREATE INDEX IF NOT EXISTS idx_bookings_customer_date_status
    ON bookings (customer_id, booking_date, status)
    WHERE customer_id IS NOT NULL AND is_deleted = false;

-- 3. Composite Index phục vụ kiểm tra trùng lịch sân và khung giờ
CREATE INDEX IF NOT EXISTS idx_bookings_pitch_slot_date
    ON bookings (pitch_id, time_slot_id, booking_date, status);

-- 4. Composite Index phục vụ truy vấn đối soát giao dịch theo thời gian, phương thức thanh toán và trạng thái
CREATE INDEX IF NOT EXISTS idx_transactions_created_method_status
    ON payment_transactions (created_at, payment_method_id, status);

-- 5. Composite Index phục vụ tra cứu nhanh lịch sử giao dịch theo đơn đặt sân
CREATE INDEX IF NOT EXISTS idx_transactions_booking_type_status
    ON payment_transactions (booking_id, payment_type, status);

