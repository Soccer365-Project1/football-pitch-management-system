package com.fpms.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin yêu cầu tạo mới hoặc cập nhật Ngày lễ")
public class HolidayRequest {

    @NotNull(message = "HOLIDAY_DATE_REQUIRED")
    @Schema(description = "Ngày lễ cụ thể (YYYY-MM-DD)", example = "2026-09-02", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate holidayDate;

    @NotBlank(message = "HOLIDAY_NAME_REQUIRED")
    @Size(max = 150, message = "HOLIDAY_NAME_MAX_LENGTH")
    @Schema(description = "Tên gọi của ngày lễ", example = "Quốc khánh 2/9", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Mô tả / Ghi chú chính sách phụ thu", example = "Nghỉ lễ Quốc khánh 2/9, áp dụng biểu giá ngày lễ")
    private String description;
}
