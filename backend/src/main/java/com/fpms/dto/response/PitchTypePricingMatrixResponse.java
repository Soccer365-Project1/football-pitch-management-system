package com.fpms.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin bảng giá của một loại sân bóng")
public class PitchTypePricingMatrixResponse {

    @Schema(description = "ID loại sân", example = "1")
    private Long pitchTypeId;

    @Schema(description = "Tên loại sân", example = "Sân 5 người")
    private String pitchTypeName;

    @Schema(description = "Sức chứa tối đa (người)", example = "10")
    private Integer playerCapacity;

    @Schema(description = "Danh sách các mức giá cấu hình của loại sân này")
    private List<PriceRateResponse> rates;
}
