package com.fpms.integration;

import com.fpms.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fpms.dto.request.BookingCreationRequest;
import com.fpms.entity.Pitch;
import com.fpms.entity.PitchType;
import com.fpms.entity.PriceMatrix;
import com.fpms.entity.TimeSlot;
import com.fpms.entity.enums.DayType;
import com.fpms.repository.PitchRepository;
import com.fpms.repository.PitchTypeRepository;
import com.fpms.repository.PriceMatrixRepository;
import com.fpms.repository.TimeSlotRepository;

import java.time.LocalTime;
import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookingScheduleIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Autowired
    private PitchRepository pitchRepository;
    @Autowired
    private PitchTypeRepository pitchTypeRepository;
    @Autowired
    private TimeSlotRepository timeSlotRepository;
    @Autowired
    private PriceMatrixRepository priceMatrixRepository;

    private Long testPitchId;
    private Long testTimeSlotId1;
    private Long testTimeSlotId2;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        PitchType pt = PitchType.builder().name("Type 1").playerCapacity(5).build();
        pt = pitchTypeRepository.save(pt);

        Pitch p = Pitch.builder().name("Pitch 1").pitchType(pt).build();
        p = pitchRepository.save(p);
        testPitchId = p.getId();

        TimeSlot ts1 = TimeSlot.builder().startTime(LocalTime.of(8,0)).endTime(LocalTime.of(9,0)).build();
        ts1 = timeSlotRepository.save(ts1);
        testTimeSlotId1 = ts1.getId();

        TimeSlot ts2 = TimeSlot.builder().startTime(LocalTime.of(9,0)).endTime(LocalTime.of(10,0)).build();
        ts2 = timeSlotRepository.save(ts2);
        testTimeSlotId2 = ts2.getId();
        
        PriceMatrix pm = PriceMatrix.builder().pitchType(pt).isPeakHour(false).dayType(DayType.WEEKDAY).price(BigDecimal.valueOf(100000)).build();
        priceMatrixRepository.save(pm);
        PriceMatrix pm2 = PriceMatrix.builder().pitchType(pt).isPeakHour(false).dayType(DayType.WEEKEND).price(BigDecimal.valueOf(120000)).build();
        priceMatrixRepository.save(pm2);
    }

    @Test
    @DisplayName("TC-I01: Integration test lấy lưới lịch đặt và bảng giá thành công")
    void getScheduleGrid_Integration_Success() throws Exception {
        LocalDate today = LocalDate.now();

        mockMvc.perform(get("/api/v1/bookings/schedule-grid")
                        .param("date", today.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.bookings").isArray())
                .andExpect(jsonPath("$.data.prices").isArray());
    }

    @Test
    @DisplayName("TC-I02: Integration test chặn ngày trong quá khứ trả về 400 và PAST_DATE_NOT_ALLOWED")
    void getScheduleGrid_PastDate_Integration_Fails() throws Exception {
        LocalDate pastDate = LocalDate.now().minusDays(1);

        mockMvc.perform(get("/api/v1/bookings/schedule-grid")
                        .param("date", pastDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.PAST_DATE_NOT_ALLOWED.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.PAST_DATE_NOT_ALLOWED.getMessage()));
    }

    @Test
    @DisplayName("TC-I03: Integration test lấy danh sách sân bóng cho khách hàng")
    void getPitches_Integration_Success() throws Exception {
        mockMvc.perform(get("/api/v1/pitches")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("TC-I04: Integration test lấy danh sách loại sân bóng")
    void getPitchTypes_Integration_Success() throws Exception {
        mockMvc.perform(get("/api/v1/pitches/types")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("TC-I05: Integration test lấy danh sách khung giờ công khai")
    void getTimeSlots_Integration_Success() throws Exception {
        mockMvc.perform(get("/api/v1/timeslots")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("TC-I06: Integration test Đặt sân thành công (Khách hàng)")
    void createBooking_Integration_Success() throws Exception {
        BookingCreationRequest request = new BookingCreationRequest();
        request.setPitchId(testPitchId);
        request.setTimeSlotId(testTimeSlotId1);
        request.setBookingDate(LocalDate.now().plusDays(2)); // Đặt ngày kia để tránh trùng các test khác
        request.setGuestName("Nguyen Van A");
        request.setGuestPhone("0901234567");

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đặt sân thành công"));
    }

    @Test
    @DisplayName("TC-I07: Integration test Đặt sân thất bại do trùng lịch")
    void createBooking_Integration_Conflict_Fails() throws Exception {
        BookingCreationRequest request = new BookingCreationRequest();
        request.setPitchId(testPitchId);
        request.setTimeSlotId(testTimeSlotId2);
        request.setBookingDate(LocalDate.now().plusDays(3));
        request.setGuestName("Nguyen Van B");
        request.setGuestPhone("0907654321");

        // Gọi lần 1: Thành công
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Gọi lần 2 (cùng thông tin): Thất bại
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.BOOKING_TIME_SLOT_ALREADY_BOOKED.getCode()));
    }
}
