package com.fpms.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCreationRequest {

    @NotNull(message = "BOOKING_PITCH_ID_REQUIRED")
    private Long pitchId;

    @NotNull(message = "BOOKING_TIME_SLOT_ID_REQUIRED")
    private Long timeSlotId;

    @NotNull(message = "BOOKING_DATE_REQUIRED")
    @FutureOrPresent(message = "BOOKING_DATE_INVALID")
    private LocalDate bookingDate;

    @NotBlank(message = "BOOKING_GUEST_NAME_REQUIRED")
    @Size(max = 100, message = "BOOKING_GUEST_NAME_TOO_LONG")
    private String guestName;

    @NotBlank(message = "BOOKING_GUEST_PHONE_REQUIRED")
    @Pattern(regexp = "^(0[35789])[0-9]{8}$", message = "BOOKING_GUEST_PHONE_INVALID")
    private String guestPhone;

    private String customerNote;
}
