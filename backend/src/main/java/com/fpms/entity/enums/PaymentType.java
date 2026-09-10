package com.fpms.entity.enums;

public enum PaymentType {
    DEPOSIT_30,     // Thanh toán tiền cọc 30% khi đặt sân
    REMAINING_70,   // Thanh toán nốt 70% sau khi kết thúc ca đá
    FULL_PAYMENT,   // Thanh toán toàn bộ 100% (áp dụng tại quầy)
    REFUND          // Giao dịch hoàn tiền cọc
}
