package com.fpms.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin chi tiết Ngày lễ trả về cho client")
public class HolidayResponse {

    @Schema(description = "ID định danh ngày lễ", example = "1")
    private Long id;

    @Schema(description = "Ngày lễ cụ thể (YYYY-MM-DD)", example = "2026-09-02")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate holidayDate;

    @Schema(description = "Tên gọi của ngày lễ", example = "Quốc khánh 2/9")
    private String name;

    @Schema(description = "Mô tả / Ghi chú chính sách phụ thu", example = "Nghỉ lễ Quốc khánh 2/9, áp dụng biểu giá ngày lễ")
    private String description;
}
