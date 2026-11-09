package com.fpms.dto.request;

import com.fpms.entity.enums.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu tìm kiếm và lọc danh sách đơn đặt sân trên Toolbar Admin")
public class AdminBookingSearchRequest {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final String DEFAULT_SORT_BY = "createdAt";
    public static final String DEFAULT_SORT_DIR = "DESC";

    // 1. Phân trang & sắp xếp
    @Min(value = 1, message = "Số trang (page) phải lớn hơn hoặc bằng 1")
    @Builder.Default
    @Schema(description = "Số trang hiện tại (bắt đầu từ 1)", example = "1", defaultValue = "1")
    private Integer page = DEFAULT_PAGE;

    @Min(value = 1, message = "Kích thước trang (size) phải lớn hơn hoặc bằng 1")
    @Max(value = 100, message = "Kích thước trang (size) tối đa là 100")
    @Builder.Default
    @Schema(description = "Số lượng bản ghi trên một trang", example = "10", defaultValue = "10")
    private Integer size = DEFAULT_PAGE_SIZE;

    @Builder.Default
    @Schema(description = "Trường cần sắp xếp", example = "createdAt", defaultValue = "createdAt")
    private String sortBy = DEFAULT_SORT_BY;

    @Builder.Default
    @Schema(description = "Chiều sắp xếp: ASC (tăng dần) hoặc DESC (giảm dần)", example = "DESC", defaultValue = "DESC")
    private String sortDir = DEFAULT_SORT_DIR;

    // 2. Bộ lọc thực tế trên Toolbar giao diện
    @Schema(description = "Từ khóa tìm kiếm nhanh (SĐT, Họ tên khách hoặc Mã đơn)", example = "0987654321")
    private String keyword;

    @Schema(description = "Lọc theo trạng thái đơn đặt sân", example = "CONFIRMED")
    private BookingStatus status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Lọc đơn từ ngày (YYYY-MM-DD)", example = "2026-10-01")
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Lọc đơn đến ngày (YYYY-MM-DD)", example = "2026-10-31")
    private LocalDate toDate;

    @Schema(description = "Lọc theo sân bóng", example = "1")
    private Long pitchId;

    public String getCleanKeyword() {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    public Pageable toPageable() {
        int pageIndex = (page != null && page > 0) ? page - 1 : 0;
        int pageSize = (size != null && size > 0) ? size : DEFAULT_PAGE_SIZE;

        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String property = StringUtils.hasText(sortBy) ? sortBy : DEFAULT_SORT_BY;

        return PageRequest.of(pageIndex, pageSize, Sort.by(direction, property));
    }
}
