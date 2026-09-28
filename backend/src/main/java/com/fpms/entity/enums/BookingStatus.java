package com.fpms.entity.enums;

public enum BookingStatus {
    PENDING_HOLD,           // Đang tạm khóa ca 10 phút chờ thanh toán tiền cọc 30%
    WAITING_APPROVAL,       // Đã cọc thành công, đang chờ nhân viên kiểm tra duyệt đơn
    CONFIRMED,              // Đã duyệt đơn thành công, ca đá chính thức được giữ
    IN_PROGRESS,            // Khách đã check-in nhận sân, trận đấu đang diễn ra
    COMPLETED,              // Trận đấu kết thúc, đã thanh toán nốt 70% và đóng ca
    WAITING_CANCELLATION,   // Khách hàng gửi yêu cầu hủy đơn và xin hoàn cọc
    CANCELLED,              // Đơn bị hủy (quá 10 phút giữ chỗ, hoặc khách tự hủy)
    REFUNDED                // Đơn hủy đã được chủ sân hoàn trả tiền cọc cho khách
}
