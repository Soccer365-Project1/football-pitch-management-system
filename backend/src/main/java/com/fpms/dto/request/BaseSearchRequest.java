package com.fpms.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Base DTO chứa các thông số tìm kiếm và phân trang dùng chung cho toàn bộ hệ thống.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Schema(description = "Yêu cầu tìm kiếm và phân trang cơ sở dùng chung")
public class BaseSearchRequest {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final String DEFAULT_SORT_BY = "createdAt";
    public static final String DEFAULT_SORT_DIR = "DESC";

    @Min(value = 1, message = "Số trang (page) phải lớn hơn hoặc bằng 1")
    @Builder.Default
    @Schema(description = "Số trang hiện tại (bắt đầu từ 1)", example = "1", defaultValue = "1")
    private Integer page = DEFAULT_PAGE;

    @Min(value = 1, message = "Kích thước trang (pageSize) phải lớn hơn hoặc bằng 1")
    @Max(value = 100, message = "Kích thước trang (pageSize) tối đa là 100")
    @Builder.Default
    @Schema(description = "Số lượng bản ghi trên một trang", example = "10", defaultValue = "10")
    private Integer pageSize = DEFAULT_PAGE_SIZE;

    @Schema(description = "Từ khóa tìm kiếm chung (tìm kiếm gần đúng)", example = "Nguyễn Văn A")
    private String keyword;

    @Builder.Default
    @Schema(description = "Trường cụ thể áp dụng tìm kiếm theo từ khóa (Ví dụ: ALL, BOOKING_CODE, PITCH_NAME, PHONE...)", example = "ALL", defaultValue = "ALL")
    private String searchField = "ALL";

    @Builder.Default
    @Schema(description = "Trường cần sắp xếp", example = "createdAt", defaultValue = "createdAt")
    private String sortBy = DEFAULT_SORT_BY;

    @Builder.Default
    @Schema(description = "Chiều sắp xếp: ASC (tăng dần) hoặc DESC (giảm dần)", example = "DESC", defaultValue = "DESC")
    private String sortDir = DEFAULT_SORT_DIR;

    /**
     * Hỗ trợ alias setSize nếu frontend gửi param 'size' thay vì 'pageSize'.
     */
    public void setSize(Integer size) {
        if (size != null) {
            this.pageSize = size;
        }
    }

    /**
     * Lấy chỉ số trang 0-indexed để truyền vào Spring Data PageRequest.
     */
    public int getZeroBasedPage() {
        int p = (page != null && page > 0) ? page : DEFAULT_PAGE;
        return Math.max(0, p - 1);
    }

    /**
     * Lấy kích thước trang hợp lệ.
     */
    public int getPageSize() {
        return (pageSize != null && pageSize > 0) ? pageSize : DEFAULT_PAGE_SIZE;
    }

    /**
     * Lấy từ khóa đã được trim bỏ khoảng trắng thừa (hoặc null nếu rỗng).
     */
    public String getCleanKeyword() {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        return keyword.trim();
    }

    /**
     * Chuyển đổi thông tin phân trang và sắp xếp sang đối tượng Spring Data Pageable.
     */
    public Pageable toPageable() {
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String property = (sortBy != null && !sortBy.trim().isEmpty()) ? sortBy.trim() : DEFAULT_SORT_BY;
        return PageRequest.of(getZeroBasedPage(), getPageSize(), Sort.by(direction, property));
    }

    /**
     * Chuyển đổi sang Pageable với trường sắp xếp mặc định tùy biến.
     */
    public Pageable toPageable(String fallbackSortBy, Sort.Direction fallbackDirection) {
        Sort.Direction direction = (sortDir != null && !sortDir.trim().isEmpty())
                ? ("ASC".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC)
                : fallbackDirection;
        String property = (sortBy != null && !sortBy.trim().isEmpty()) ? sortBy.trim() : fallbackSortBy;
        return PageRequest.of(getZeroBasedPage(), getPageSize(), Sort.by(direction, property));
    }
}
