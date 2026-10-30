package com.fpms.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 1. Nhóm Lỗi Hệ Thống & Giao Thức (1000 - 1999)
    UNCATEGORIZED_EXCEPTION(1000, HttpStatus.INTERNAL_SERVER_ERROR, "Đã có lỗi không xác định xảy ra, vui lòng liên hệ quản trị viên"),
    INVALID_KEY(1001, HttpStatus.BAD_REQUEST, "Tham số yêu cầu không hợp lệ"),
    RESOURCE_NOT_FOUND(1002, HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên yêu cầu trên hệ thống"),
    METHOD_NOT_ALLOWED(1003, HttpStatus.METHOD_NOT_ALLOWED, "Phương thức HTTP không được hỗ trợ cho đường dẫn này"),
    JSON_PARSE_ERROR(1004, HttpStatus.BAD_REQUEST, "Cấu trúc dữ liệu JSON gửi lên không đúng định dạng"),
    DATA_INTEGRITY_VIOLATION(1005, HttpStatus.CONFLICT, "Dữ liệu bị trùng lặp hoặc vi phạm ràng buộc cơ sở dữ liệu"),

    // 2. Nhóm Lỗi Xác Thực & Người Dùng (2000 - 2999)
    USER_ALREADY_EXISTS(2001, HttpStatus.BAD_REQUEST, "Email hoặc số điện thoại đã được đăng ký trong hệ thống"),
    PHONE_ALREADY_EXISTS(2002, HttpStatus.BAD_REQUEST, "Số điện thoại đã tồn tại trên hệ thống"),
    EMAIL_ALREADY_EXISTS(2003, HttpStatus.BAD_REQUEST, "Email đã tồn tại trên hệ thống"),
    INVALID_CREDENTIALS(2004, HttpStatus.UNAUTHORIZED, "Sai tài khoản hoặc mật khẩu"),
    USER_LOCKED(2005, HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên"),
    USER_NOT_FOUND(2006, HttpStatus.NOT_FOUND, "Không tìm thấy thông tin tài khoản người dùng"),
    PASSWORD_CONFIRM_NOT_MATCH(2007, HttpStatus.BAD_REQUEST, "Mật khẩu xác nhận không trùng khớp"),
    ROLE_NOT_FOUND(2008, HttpStatus.INTERNAL_SERVER_ERROR, "Không tìm thấy vai trò người dùng mặc định"),
    UNAUTHORIZED(2009, HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện chức năng này"),
    INVALID_TOKEN(2010, HttpStatus.UNAUTHORIZED, "Mã xác thực không hợp lệ hoặc đã hết hạn"),
    OTP_INVALID(2011, HttpStatus.BAD_REQUEST, "Mã xác thực OTP không chính xác"),
    OTP_EXPIRED(2012, HttpStatus.BAD_REQUEST, "Mã xác thực OTP đã hết hạn. Vui lòng yêu cầu gửi lại mã mới"),
    OTP_ALREADY_USED(2013, HttpStatus.BAD_REQUEST, "Mã xác thực OTP này đã được sử dụng"),
    OTP_COOLDOWN_ACTIVE(2014, HttpStatus.TOO_MANY_REQUESTS, "Vui lòng chờ 60 giây trước khi yêu cầu gửi lại mã OTP mới"),
    EMAIL_SEND_FAILED(2015, HttpStatus.INTERNAL_SERVER_ERROR, "Không thể gửi email xác thực, vui lòng thử lại sau"),
    OLD_PASSWORD_INCORRECT(2016, HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không chính xác"),
    NEW_PASSWORD_SAME_AS_OLD(2017, HttpStatus.BAD_REQUEST, "Mật khẩu mới không được trùng với mật khẩu hiện tại"),
    CANNOT_LOCK_SELF(2018, HttpStatus.BAD_REQUEST, "Bạn không thể tự khóa tài khoản của chính mình"),
    CANNOT_LOCK_ADMIN(2019, HttpStatus.BAD_REQUEST, "Không thể khóa tài khoản của Quản trị viên khác"),
    CANNOT_CHANGE_OWN_ROLE(2020, HttpStatus.BAD_REQUEST, "Bạn không thể tự thay đổi vai trò của chính mình"),
    CANNOT_MODIFY_ADMIN(2021, HttpStatus.BAD_REQUEST, "Không thể thay đổi vai trò của Quản trị viên khác"),
    INVALID_ROLE_ASSIGNMENT(2022, HttpStatus.BAD_REQUEST, "Chỉ được phép phân quyền Khách hàng (ROLE_CUSTOMER) hoặc Nhân viên (ROLE_STAFF)"),
    STAFF_CANNOT_MODIFY_OTHER_STAFF(2023, HttpStatus.FORBIDDEN, "Nhân viên không có quyền chỉnh sửa thông tin của nhân viên khác hoặc Quản trị viên"),
    STAFF_CANNOT_LOCK_OTHER_STAFF(2024, HttpStatus.FORBIDDEN, "Nhân viên không có quyền khóa hoặc mở khóa tài khoản của nhân viên khác hoặc Quản trị viên"),

    // 3. Nhóm Lỗi Phân Hệ Sân Bóng & Khung Giờ (3000 - 3999)
    PITCH_NOT_FOUND(3001, HttpStatus.NOT_FOUND, "Không tìm thấy thông tin sân bóng"),
    PITCH_NAME_ALREADY_EXISTS(3002, HttpStatus.BAD_REQUEST, "Tên sân bóng đã tồn tại trên hệ thống"),
    PITCH_TYPE_NOT_FOUND(3003, HttpStatus.NOT_FOUND, "Không tìm thấy loại sân bóng yêu cầu"),
    PITCH_STATUS_INVALID(3004, HttpStatus.BAD_REQUEST, "Trạng thái sân bóng không hợp lệ"),
    PITCH_HAS_BOOKINGS(3005, HttpStatus.BAD_REQUEST, "Sân bóng đã có ca đá hoặc lịch đặt trong hệ thống, không thể xóa. Vui lòng chuyển trạng thái sang Ngừng hoạt động"),
    PITCH_NOT_ACTIVE(3007, HttpStatus.BAD_REQUEST, "Sân bóng đang bảo trì hoặc dừng hoạt động, không thể đặt sân"),
    TIME_SLOT_NOT_FOUND(3011, HttpStatus.NOT_FOUND, "Không tìm thấy thông tin khung giờ"),
    TIME_SLOT_INVALID_TIME(3012, HttpStatus.BAD_REQUEST, "Giờ kết thúc phải lớn hơn giờ bắt đầu"),
    TIME_SLOT_OVERLAPPING(3013, HttpStatus.BAD_REQUEST, "Khung giờ bị chồng chéo với khung giờ khác đã tồn tại"),
    TIME_SLOT_HAS_ACTIVE_BOOKINGS(3014, HttpStatus.BAD_REQUEST, "Không thể xóa khung giờ do đang có đơn đặt chưa hoàn tất"),
    PAST_DATE_NOT_ALLOWED(3999, HttpStatus.BAD_REQUEST, "Không thể xem hoặc đặt lịch cho ngày trong quá khứ"),
    TIME_SLOT_INVALID_DURATION(3015, HttpStatus.BAD_REQUEST, "Thời lượng ca đá phải từ 1 giờ (60 phút) đến 2 giờ (120 phút)"),

    BOOKING_PITCH_ID_REQUIRED(3998, HttpStatus.BAD_REQUEST, "Vui lòng chọn sân bóng"),
    BOOKING_TIME_SLOT_ID_REQUIRED(3997, HttpStatus.BAD_REQUEST, "Vui lòng chọn khung giờ"),
    BOOKING_DATE_REQUIRED(3996, HttpStatus.BAD_REQUEST, "Vui lòng chọn ngày đặt sân"),
    BOOKING_DATE_INVALID(3995, HttpStatus.BAD_REQUEST, "Ngày đặt sân phải là hôm nay hoặc trong tương lai"),
    BOOKING_GUEST_NAME_REQUIRED(3994, HttpStatus.BAD_REQUEST, "Vui lòng nhập tên khách hàng"),
    BOOKING_GUEST_NAME_TOO_LONG(3993, HttpStatus.BAD_REQUEST, "Tên khách hàng không được vượt quá 100 ký tự"),
    BOOKING_GUEST_PHONE_REQUIRED(3992, HttpStatus.BAD_REQUEST, "Vui lòng nhập số điện thoại"),
    BOOKING_GUEST_PHONE_INVALID(3991, HttpStatus.BAD_REQUEST, "Số điện thoại không đúng định dạng"),
    BOOKING_TIME_SLOT_ALREADY_BOOKED(3990, HttpStatus.BAD_REQUEST, "Ca đá này đã có người đặt, vui lòng chọn khung giờ khác"),
    TIME_SLOT_ALREADY_PASSED(3989, HttpStatus.BAD_REQUEST, "Ca đá này đã kết thúc, vui lòng chọn ca khác"),

    // Holiday Error Codes (3020 - 3029)
    HOLIDAY_NOT_FOUND(3021, HttpStatus.NOT_FOUND, "Không tìm thấy thông tin ngày lễ"),
    HOLIDAY_DATE_ALREADY_EXISTS(3022, HttpStatus.CONFLICT, "Ngày lễ này đã tồn tại trong hệ thống"),
    HOLIDAY_DATE_REQUIRED(3023, HttpStatus.BAD_REQUEST, "Ngày lễ không được để trống"),
    HOLIDAY_NAME_REQUIRED(3024, HttpStatus.BAD_REQUEST, "Tên ngày lễ không được để trống"),
    HOLIDAY_NAME_MAX_LENGTH(3025, HttpStatus.BAD_REQUEST, "Tên ngày lễ không được vượt quá 150 ký tự"),

    // Pricing Matrix Error Codes (3030 - 3039)
    PRICE_MATRIX_NOT_FOUND(3031, HttpStatus.NOT_FOUND, "Không tìm thấy thông tin cấu hình giá"),
    PRICE_INVALID(3032, HttpStatus.BAD_REQUEST, "Giá thuê sân phải là số dương tối thiểu từ 1.000 VNĐ"),
    PRICE_REQUIRED(3033, HttpStatus.BAD_REQUEST, "Mức giá không được để trống"),
    PRICE_ITEMS_REQUIRED(3034, HttpStatus.BAD_REQUEST, "Danh sách các mức giá cấu hình không được để trống"),
    PRICE_MATRIX_DUPLICATE(3035, HttpStatus.CONFLICT, "Cấu hình giá cho loại sân, khung giờ và loại ngày này đã tồn tại"),
    DAY_TYPE_REQUIRED(3036, HttpStatus.BAD_REQUEST, "Loại ngày không được để trống"),
    PITCH_TYPE_ID_REQUIRED(3006, HttpStatus.BAD_REQUEST, "Loại sân không được để trống"),
    IS_PEAK_HOUR_REQUIRED(3016, HttpStatus.BAD_REQUEST, "Vui lòng xác định khung giờ vàng hay thường"),
    PRICE_MAX_EXCEEDED(3037, HttpStatus.BAD_REQUEST, "Giá thuê sân không được vượt quá 50.000.000 VNĐ"),

    BOOKING_NOT_FOUND(3040, HttpStatus.NOT_FOUND, "Không tìm thấy thông tin đơn đặt sân"),
    PAYMENT_METHOD_NOT_FOUND(3041, HttpStatus.NOT_FOUND, "Không tìm thấy phương thức thanh toán"),
    PAYMENT_TRANSACTION_NOT_FOUND(3042, HttpStatus.NOT_FOUND, "Không tìm thấy thông tin giao dịch thanh toán"),
    PAYMENT_FAILED(3043, HttpStatus.BAD_REQUEST, "Thanh toán thất bại"),
    CANNOT_CANCEL_BOOKING_STATUS(3044, HttpStatus.BAD_REQUEST, "Chỉ có thể hủy đơn đặt sân ở trạng thái chờ xác nhận hoặc đã xác nhận"),
    BOOKING_ALREADY_STARTED(3045, HttpStatus.BAD_REQUEST, "Không thể hủy đơn đặt sân khi ca đá đã bắt đầu hoặc đã qua"),
    BOOKING_CANCEL_REASON_REQUIRED(3046, HttpStatus.BAD_REQUEST, "Lý do hủy đơn không được để trống"),
    BOOKING_CANCEL_REASON_TOO_LONG(3047, HttpStatus.BAD_REQUEST, "Lý do hủy đơn không được vượt quá 500 ký tự"),
    CANNOT_CANCEL_WITHIN_24_HOURS(3048, HttpStatus.BAD_REQUEST, "Không thể hủy đơn đặt sân khi thời gian đến ca đá còn dưới 24 giờ"),
    BOOKING_CANNOT_REPAY(3049, HttpStatus.BAD_REQUEST, "Chỉ có thể thanh toán lại cho đơn đặt sân đang chờ đặt cọc"),
    HOLDING_TIME_EXPIRED(3050, HttpStatus.BAD_REQUEST, "Thời gian giữ chỗ của đơn đặt sân đã hết hạn. Vui lòng đặt lại đơn mới"),
    HOLDING_TIME_TOO_SHORT(3051, HttpStatus.BAD_REQUEST, "Thời gian giữ chỗ còn lại dưới 2 phút không đủ để thanh toán. Đơn đặt sân đã được hủy, vui lòng chọn lại khung giờ mới"),
    INVALID_BOOKING_STATUS_FOR_APPROVAL(3052, HttpStatus.BAD_REQUEST, "Chỉ có thể duyệt đơn đặt sân đang ở trạng thái chờ duyệt"),
    INVALID_BOOKING_STATUS_FOR_CHECK_IN(3053, HttpStatus.BAD_REQUEST, "Chỉ có thể nhận sân cho đơn đặt sân đã được xác nhận"),
    CHECK_IN_TIME_NOT_REACHED(3054, HttpStatus.BAD_REQUEST, "Chưa đến giờ nhận sân (giờ nhận sân phải từ giờ bắt đầu ca đá)"),
    BOOKING_DATE_NOT_TODAY(3055, HttpStatus.BAD_REQUEST, "Chỉ có thể nhận sân cho các ca đá diễn ra trong ngày hôm nay"),
    INVALID_BOOKING_STATUS_FOR_CHECK_OUT(3056, HttpStatus.BAD_REQUEST, "Chỉ có thể kết thúc và trả sân cho ca đá đang diễn ra (đang sử dụng)"),
    PAYMENT_METHOD_NOT_SUPPORTED_FOR_CHECKOUT(3057, HttpStatus.BAD_REQUEST, "Chỉ hỗ trợ thanh toán nốt bằng Tiền mặt (CASH) hoặc Chuyển khoản (BANK_TRANSFER)"),
    CHECKOUT_PAYMENT_METHOD_REQUIRED(3058, HttpStatus.BAD_REQUEST, "Vui lòng chọn hình thức thanh toán nốt"),
    CHECKOUT_NOTE_TOO_LONG(3059, HttpStatus.BAD_REQUEST, "Ghi chú trả sân không được vượt quá 500 ký tự"),
    BOOKING_CUSTOMER_NOTE_TOO_LONG(3988, HttpStatus.BAD_REQUEST, "Ghi chú đặt sân không được vượt quá 500 ký tự"),
    TRANSACTION_BOOKING_REQUIRED(3060, HttpStatus.BAD_REQUEST, "Thông tin đơn đặt sân của giao dịch không được để trống"),
    TRANSACTION_AMOUNT_REQUIRED(3061, HttpStatus.BAD_REQUEST, "Số tiền giao dịch không được để trống"),
    TRANSACTION_PAYMENT_TYPE_REQUIRED(3062, HttpStatus.BAD_REQUEST, "Loại thanh toán không được để trống"),
    TRANSACTION_NOTE_TOO_LONG(3063, HttpStatus.BAD_REQUEST, "Ghi chú giao dịch không được vượt quá 1000 ký tự"),

    // Report & Analytics Error Codes (5000 - 5099)
    INVALID_DATE_RANGE(5001, HttpStatus.BAD_REQUEST, "Khoảng thời gian không hợp lệ. Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");


    private final int code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(int code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
