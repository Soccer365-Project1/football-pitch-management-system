package com.fpms.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Yêu cầu lưu cấu hình bảng giá cho một loại sân bóng")
public class SavePitchTypePricingRequest {

    @NotEmpty(message = "PRICE_ITEMS_REQUIRED")
    @Schema(description = "Danh sách các mức giá cấu hình cho loại sân này", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<@Valid PriceRateItemRequest> rates;
}
