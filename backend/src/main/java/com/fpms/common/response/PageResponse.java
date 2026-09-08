package com.fpms.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {

    private int pageNo;          // Trang hiện tại (bắt đầu từ 1)
    private int pageSize;        // Số bản ghi trên 1 trang
    private long totalElements;  // Tổng số bản ghi thỏa mãn điều kiện
    private int totalPages;      // Tổng số trang
    private boolean isLast;      // Đã đến trang cuối cùng chưa
    private List<T> items;       // Danh sách dữ liệu

    /**
     * Chuyển đổi trực tiếp từ Page của Spring Data JPA sang PageResponse
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .pageNo(page.getNumber() + 1) // Chuyển từ 0-indexed sang 1-indexed cho Client
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .items(page.getContent())
                .build();
    }
}
