package com.fpms.controller;

import com.fpms.common.response.ApiResponse;
import com.fpms.dto.response.PriceItemResponse;
import com.fpms.dto.response.ScheduleGridItemResponse;
import com.fpms.dto.response.ScheduleGridResponse;
import com.fpms.entity.Booking;
import com.fpms.entity.PriceMatrix;
import com.fpms.entity.enums.DayType;
import com.fpms.repository.BookingRepository;
import com.fpms.repository.HolidayRepository;
import com.fpms.repository.PriceMatrixRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired
    private PriceMatrixRepository priceMatrixRepository;

    private DayType determineDayType(LocalDate date) {
        if (holidayRepository.existsByHolidayDate(date)) {
            return DayType.HOLIDAY;
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return DayType.WEEKEND;
        }
        return DayType.WEEKDAY;
    }

    @GetMapping("/schedule-grid")
    public ResponseEntity<ApiResponse<ScheduleGridResponse>> getScheduleGrid(
            @RequestParam LocalDate date,
            @RequestParam(required = false) Long pitchTypeId
    ) {
        if (date.isBefore(LocalDate.now())) {
            throw new com.fpms.exception.AppException(com.fpms.exception.ErrorCode.PAST_DATE_NOT_ALLOWED);
        }
        
        ScheduleGridResponse response = new ScheduleGridResponse();

        // 1. Dữ liệu Bookings
        List<Booking> bookings = bookingRepository.findOccupyingBookings(date, pitchTypeId);
        
        List<ScheduleGridItemResponse> gridItems = bookings.stream()
                .map(b -> new ScheduleGridItemResponse(
                        b.getPitch().getId(), 
                        b.getTimeSlot().getId(), 
                        b.getStatus().name()
                ))
                .collect(Collectors.toList());
                
        response.setBookings(gridItems);

        // 2. Dữ liệu Bảng giá
        DayType currentDayType = determineDayType(date);
        List<PriceMatrix> matrices = priceMatrixRepository.findByDayType(currentDayType);
        
        List<PriceItemResponse> priceItems = matrices.stream()
                .map(m -> new PriceItemResponse(m.getPitchType().getId(), m.getIsPeakHour(), m.getPrice()))
                .collect(Collectors.toList());
                
        response.setPrices(priceItems);

        return ResponseEntity.ok(ApiResponse.success("Success", response));
    }
}
