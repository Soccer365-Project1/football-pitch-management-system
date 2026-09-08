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
    DATA_INTEGRITY_VIOLATION(1005, HttpStatus.CONFLICT, "Dữ liệu bị trùng lặp hoặc vi phạm ràng buộc cơ sở dữ liệu");


    private final int code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(int code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
