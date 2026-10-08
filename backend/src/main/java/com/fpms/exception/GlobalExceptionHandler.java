package com.fpms.exception;

import com.fpms.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 1. Xử lý ngoại lệ nghiệp vụ chủ động (AppException)
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Object>> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.warn("[AppException] Code: {} | Message: {}", errorCode.getCode(), ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(errorCode.getCode(), ex.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    /**
     * 2. Xử lý ngoại lệ kiểm tra dữ liệu đầu vào (DTO @Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException ex) {
        List<FieldErrorDetail> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            String defaultMessage = error.getDefaultMessage();
            String resolvedMessage = defaultMessage;
            if (defaultMessage != null) {
                try {
                    ErrorCode errorCode = ErrorCode.valueOf(defaultMessage);
                    resolvedMessage = errorCode.getMessage();
                } catch (IllegalArgumentException ignored) {
                    // Không phải tên ErrorCode enum, giữ nguyên defaultMessage
                }
            }
            errors.add(FieldErrorDetail.builder()
                    .field(error.getField())
                    .message(resolvedMessage)
                    .rejectedValue(error.getRejectedValue())
                    .build());
        });

        log.warn("[ValidationException] Có {} trường dữ liệu không hợp lệ", errors.size());

        ApiResponse<Object> response = ApiResponse.error(400, "Dữ liệu đầu vào không hợp lệ", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 3. Xử lý vi phạm ràng buộc trên @RequestParam hoặc @PathVariable
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldErrorDetail> errors = new ArrayList<>();
        ex.getConstraintViolations().forEach(violation -> {
            errors.add(FieldErrorDetail.builder()
                    .field(violation.getPropertyPath().toString())
                    .message(violation.getMessage())
                    .rejectedValue(violation.getInvalidValue())
                    .build());
        });

        ApiResponse<Object> response = ApiResponse.error(400, "Tham số truy vấn không hợp lệ", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 4. Xử lý lỗi cú pháp JSON hoặc parse Enum sai
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("[JsonParseError] Request body JSON bị lỗi cú pháp: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error(
                ErrorCode.JSON_PARSE_ERROR.getCode(),
                "Dữ liệu JSON gửi lên sai định dạng hoặc giá trị Enum không hợp lệ"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 5. Xử lý thiếu param bắt buộc hoặc truyền sai kiểu dữ liệu param (@RequestParam)
     */
    @ExceptionHandler({
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleRequestParamException(Exception ex) {
        log.warn("[RequestParamException] Lỗi tham số truy vấn: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error(400, "Tham số truy vấn không hợp lệ hoặc bị thiếu");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 6. Xử lý lỗi không tìm thấy endpoint (404)
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResourceFound(NoResourceFoundException ex) {
        ApiResponse<Object> response = ApiResponse.error(
                ErrorCode.RESOURCE_NOT_FOUND.getCode(),
                "Đường dẫn API hoặc tài nguyên không tồn tại: " + ex.getResourcePath()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * 7. Xử lý gọi sai HTTP Method (405 Method Not Allowed)
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        ApiResponse<Object> response = ApiResponse.error(
                ErrorCode.METHOD_NOT_ALLOWED.getCode(),
                "Phương thức " + ex.getMethod() + " không được hỗ trợ cho API này"
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    /**
     * 7. Xử lý lỗi từ chối truy cập (403 Forbidden / Access Denied từ @PreAuthorize)
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        log.warn("[AccessDenied] Người dùng không có quyền truy cập tài nguyên này: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error(
                ErrorCode.UNAUTHORIZED.getCode(),
                ErrorCode.UNAUTHORIZED.getMessage()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * 8. Xử lý vi phạm ràng buộc toàn vẹn CSDL (DataIntegrityViolationException / numeric overflow)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("[DataIntegrityViolation] Vi phạm ràng buộc dữ liệu: {}", ex.getMessage());
        
        // Kiểm tra xem có phải lỗi do cái index unique_active_booking gây ra không
        String message = ex.getMessage();
        if (message != null && message.contains("unique_active_booking")) {
            ApiResponse<Object> response = ApiResponse.error(
                    ErrorCode.TIME_SLOT_OVERLAPPING.getCode(),
                    "Rất tiếc! Ca sân này vừa có người nhanh tay đặt mất. Vui lòng chọn ca khác."
            );
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        
        // Các lỗi DataIntegrity khác (vd thiếu khóa ngoại, duplicate key khác...)
        ApiResponse<Object> response = ApiResponse.error(
                ErrorCode.UNCATEGORIZED_EXCEPTION.getCode(),
                "Dữ liệu không hợp lệ hoặc đã tồn tại"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 9. Bắt toàn bộ lỗi bất ngờ khác (Unhandled 500) - Che giấu Stacktrace, sinh Trace ID
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log.error("[CRITICAL SYSTEM ERROR] TraceID: {} | Error: {}", traceId, ex.getMessage(), ex);

        ApiResponse<Object> response = ApiResponse.error(
                ErrorCode.UNCATEGORIZED_EXCEPTION.getCode(),
                "Đã có lỗi hệ thống xảy ra. Vui lòng cung cấp mã hỗ trợ [TraceID: " + traceId + "] cho quản trị viên"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
