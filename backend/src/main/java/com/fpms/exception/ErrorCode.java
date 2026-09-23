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

    // 3. Nhóm Lỗi Phân Hệ Sân Bóng & Khung Giờ (3000 - 3999)
    PITCH_NOT_FOUND(3001, HttpStatus.NOT_FOUND, "Không tìm thấy thông tin sân bóng"),
    PITCH_NAME_ALREADY_EXISTS(3002, HttpStatus.BAD_REQUEST, "Tên sân bóng đã tồn tại trên hệ thống"),
    PITCH_TYPE_NOT_FOUND(3003, HttpStatus.NOT_FOUND, "Không tìm thấy loại sân bóng yêu cầu"),
    PITCH_STATUS_INVALID(3004, HttpStatus.BAD_REQUEST, "Trạng thái sân bóng không hợp lệ"),
    TIME_SLOT_NOT_FOUND(3011, HttpStatus.NOT_FOUND, "Không tìm thấy thông tin khung giờ"),
    TIME_SLOT_INVALID_TIME(3012, HttpStatus.BAD_REQUEST, "Giờ kết thúc phải lớn hơn giờ bắt đầu"),
    TIME_SLOT_OVERLAPPING(3013, HttpStatus.BAD_REQUEST, "Khung giờ bị chồng chéo với khung giờ khác đã tồn tại"),
    TIME_SLOT_HAS_ACTIVE_BOOKINGS(3014, HttpStatus.BAD_REQUEST, "Không thể xóa khung giờ do đang có đơn đặt chưa hoàn tất");


    private final int code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(int code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
