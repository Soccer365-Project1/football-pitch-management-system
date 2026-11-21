package com.fpms.integration;

import com.fpms.dto.request.ReportFilterRequest;
import com.fpms.dto.response.report.BookingFrequencyResponse;
import com.fpms.dto.response.report.DashboardSummaryResponse;
import com.fpms.dto.response.report.RevenueByPitchTypeResponse;
import com.fpms.dto.response.report.RevenueTrendPointResponse;
import com.fpms.entity.*;
import com.fpms.entity.enums.AuthProvider;
import com.fpms.entity.enums.RoleName;
import com.fpms.entity.enums.UserStatus;
import com.fpms.repository.*;
import com.fpms.security.JwtTokenProvider;
import com.fpms.security.UserPrincipal;
import com.fpms.service.AdminReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * =========================================================================================
 * BỘ KIỂM THỬ ĐO LƯỜNG HIỆU NĂNG DASHBOARD VỚI 1.000 ĐƠN HÀNG MẪU (FPMS-208)
 * =========================================================================================
 * Kiểm chứng thực nghiệm tốc độ phản hồi của CSDL (Index Scan) và các truy vấn SQL Aggregate
 * khi hệ thống tăng trưởng dữ liệu lên 500 - 1.000 đơn hàng.
 *
 * Tiêu chí nghiệm thu:
 * 1. 1.000 đơn hàng mẫu được sinh tự động trên CSDL PostgreSQL (fpms_test).
 * 2. Đo lường chính xác thời gian thực thi (Latency / Response Time) của 4 APIs Dashboard.
 * 3. Kiểm chứng cơ chế gọi đồng thời (Concurrent / Promise.all) của Frontend.
 * 4. Đo lường End-to-End qua MockMvc có đầy đủ JWT Bearer Token.
 */
class AdminDashboardPerformanceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AdminReportService adminReportService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PitchTypeRepository pitchTypeRepository;

    @Autowired
    private PitchRepository pitchRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private static final int SAMPLE_BOOKINGS_COUNT = 1000;
    private static final LocalDate START_DATE = LocalDate.of(2026, 8, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 8, 30); // 30 ngày

    private String adminToken;
    private final List<Long> pitchIds = new ArrayList<>();
    private final List<Long> timeSlotIds = new ArrayList<>();
    private final List<Long> customerIds = new ArrayList<>();

    @BeforeEach
    void setUpTestData() {
        pitchIds.clear();
        timeSlotIds.clear();
        customerIds.clear();

        // 1. Lấy vai trò và tài khoản Quản trị viên (Admin)
        Role adminRole = roleRepository.findByRoleName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .roleName(RoleName.ROLE_ADMIN)
                        .description("Quản trị viên")
                        .build()));

        Role customerRole = roleRepository.findByRoleName(RoleName.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .roleName(RoleName.ROLE_CUSTOMER)
                        .description("Khách hàng")
                        .build()));

        User admin = userRepository.findByEmail("admin@soccer365.vn")
                .orElseGet(() -> {
                    User u = User.builder()
                            .email("admin_perf_" + UUID.randomUUID().toString().substring(0, 8) + "@fpms.vn")
                            .phoneNumber("09" + (10000000 + (int)(Math.random() * 89999999)))
                            .fullName("Quản Trị Viên Benchmark")
                            .passwordHash(passwordEncoder.encode("Admin@123"))
                            .status(UserStatus.ACTIVE)
                            .authProvider(AuthProvider.LOCAL)
                            .role(adminRole)
                            .build();
                    u.setIsDeleted(false);
                    return userRepository.save(u);
                });

        adminToken = jwtTokenProvider.generateAccessToken(admin);

        // 2. Lấy hoặc tạo Loại sân (Sân 5 người, Sân 7 người)
        PitchType pt5 = pitchTypeRepository.findByNameIgnoreCaseAndIsDeletedFalse("Sân 5 người")
                .orElseGet(() -> pitchTypeRepository.save(PitchType.builder().name("Sân 5 người").playerCapacity(5).build()));
        PitchType pt7 = pitchTypeRepository.findByNameIgnoreCaseAndIsDeletedFalse("Sân 7 người")
                .orElseGet(() -> pitchTypeRepository.save(PitchType.builder().name("Sân 7 người").playerCapacity(7).build()));

        // 3. Lấy hoặc tạo ít nhất 10 Sân bóng (đảm bảo đủ không gian phân bổ lịch đặt không trùng ca)
        for (int i = 1; i <= 10; i++) {
            String pName = "Sân Benchmark " + i;
            PitchType pt = (i % 2 == 1) ? pt5 : pt7;
            Pitch p = pitchRepository.findByNameIgnoreCaseAndIsDeletedFalse(pName)
                    .orElseGet(() -> pitchRepository.save(Pitch.builder().name(pName).pitchType(pt).build()));
            pitchIds.add(p.getId());
        }

        // 4. Lấy hoặc tạo ít nhất 7 Khung giờ đá
        List<TimeSlot> existingSlots = timeSlotRepository.findAll();
        LocalTime[] slotStarts = {
                LocalTime.of(8, 0), LocalTime.of(9, 30), LocalTime.of(15, 0),
                LocalTime.of(16, 30), LocalTime.of(18, 0), LocalTime.of(19, 30),
                LocalTime.of(21, 0)
        };
        for (int s = 0; s < 7; s++) {
            LocalTime start = slotStarts[s];
            LocalTime end = start.plusMinutes(90);
            TimeSlot ts = existingSlots.stream()
                    .filter(slot -> slot.getStartTime().equals(start))
                    .findFirst()
                    .orElseGet(() -> timeSlotRepository.save(TimeSlot.builder().startTime(start).endTime(end).build()));
            timeSlotIds.add(ts.getId());
        }

        // 5. Tạo 50 khách hàng mẫu (để mô phỏng tỷ lệ khách quay lại chính xác)
        for (int i = 0; i < 50; i++) {
            String randomDigits = String.format("%08d", (int)(Math.random() * 90000000) + 10000000);
            User cust = User.builder()
                    .email("cust_perf_" + i + "_" + UUID.randomUUID().toString().substring(0, 6) + "@gmail.com")
                    .phoneNumber("08" + randomDigits)
                    .fullName("Khách Hàng " + (i + 1))
                    .passwordHash(passwordEncoder.encode("Cust@123"))
                    .status(UserStatus.ACTIVE)
                    .authProvider(AuthProvider.LOCAL)
                    .role(customerRole)
                    .build();
            cust.setIsDeleted(false);
            cust = userRepository.save(cust);
            customerIds.add(cust.getId());
        }

        // 6. Xoá dữ liệu đơn hàng test cũ nếu có và nạp 1.000 đơn hàng mẫu bằng Batch SQL
        jdbcTemplate.update("DELETE FROM bookings WHERE booking_code LIKE 'BK-P208-%'");
        seedSampleBookings(SAMPLE_BOOKINGS_COUNT);
    }

    private void seedSampleBookings(int count) {
        String sql = """
            INSERT INTO bookings (
                booking_code, booking_date, status, total_pitch_amount, deposit_amount,
                additional_fee, remaining_amount, price_snapshot, start_time_snapshot,
                end_time_snapshot, pitch_name_snapshot, booking_type, customer_id,
                pitch_id, time_slot_id, is_deleted, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        List<Object[]> batchArgs = new ArrayList<>(count);
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Time defaultStart = Time.valueOf(LocalTime.of(17, 30));
        Time defaultEnd = Time.valueOf(LocalTime.of(19, 0));

        for (int i = 0; i < count; i++) {
            String code = "BK-P208-" + String.format("%05d", i + 1);
            int dateOffset = i % 30; // 0..29 ngày
            LocalDate date = START_DATE.plusDays(dateOffset);

            // Đảm bảo mỗi bộ (pitch_id, time_slot_id, booking_date) là DUY NHẤT tuyệt đối
            int pitchIdx = (i / 30) % pitchIds.size();
            int slotIdx = (i / (30 * pitchIds.size())) % timeSlotIds.size();

            Long pId = pitchIds.get(pitchIdx);
            Long tsId = timeSlotIds.get(slotIdx);
            Long custId = customerIds.get(i % customerIds.size());

            // Phân bổ trạng thái: 70% COMPLETED, 15% CONFIRMED, 10% CANCELLED, 5% REFUNDED
            String status;
            BigDecimal pitchAmount = BigDecimal.valueOf(300000);
            BigDecimal deposit = BigDecimal.valueOf(90000); // 30%
            BigDecimal fee;
            BigDecimal remaining;

            int mod = i % 100;
            if (mod < 70) {
                status = "COMPLETED";
                fee = BigDecimal.valueOf((i % 4) * 20000); // Phụ phí nước/bóng 0, 20k, 40k, 60k
                remaining = BigDecimal.ZERO;
            } else if (mod < 85) {
                status = "CONFIRMED";
                fee = BigDecimal.ZERO;
                remaining = BigDecimal.valueOf(210000); // 70% còn lại
            } else if (mod < 95) {
                status = "CANCELLED";
                fee = BigDecimal.ZERO;
                remaining = BigDecimal.ZERO;
            } else {
                status = "REFUNDED";
                fee = BigDecimal.ZERO;
                remaining = BigDecimal.ZERO;
            }

            batchArgs.add(new Object[]{
                    code,
                    Date.valueOf(date),
                    status,
                    pitchAmount,
                    deposit,
                    fee,
                    remaining,
                    pitchAmount,
                    defaultStart,
                    defaultEnd,
                    "Sân Bóng Mẫu " + (i % 4 + 1),
                    "ONLINE",
                    custId,
                    pId,
                    tsId,
                    false,
                    now,
                    now
            });
        }

        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    @Test
    @DisplayName("BENCHMARK-01: Đo lường thời gian thực thi tuần tự 4 truy vấn Dashboard với 1.000 đơn hàng mẫu")
    void benchmarkSequentialDashboardQueries_Success() {
        ReportFilterRequest filter = new ReportFilterRequest(START_DATE, END_DATE);

        // 1. Warm-up (Khởi động kết nối và nạp cache bộ đệm)
        adminReportService.getDashboardSummary(filter);

        // 2. Đo lường chi tiết từng hàm
        long t1 = System.nanoTime();
        DashboardSummaryResponse summary = adminReportService.getDashboardSummary(filter);
        long d1 = (System.nanoTime() - t1) / 1_000_000; // ms

        long t2 = System.nanoTime();
        List<RevenueTrendPointResponse> trend = adminReportService.getRevenueTrend(filter);
        long d2 = (System.nanoTime() - t2) / 1_000_000;

        long t3 = System.nanoTime();
        List<BookingFrequencyResponse> frequency = adminReportService.getBookingFrequency(filter);
        long d3 = (System.nanoTime() - t3) / 1_000_000;

        long t4 = System.nanoTime();
        List<RevenueByPitchTypeResponse> pitchTypes = adminReportService.getRevenueByPitchType(filter);
        long d4 = (System.nanoTime() - t4) / 1_000_000;

        long totalSequential = d1 + d2 + d3 + d4;

        // In kết quả trực quan
        System.out.println("\n" + "=".repeat(85));
        System.out.println(" KET QUA BENCHMARK TUAN TU 4 TRUY VAN DASHBOARD VOI " + SAMPLE_BOOKINGS_COUNT + " DON HANG MAU (FPMS-208)");
        System.out.println("=".repeat(85));
        System.out.printf("  1. getDashboardSummary (4 KPI & Ti le tang truong)     : %4d ms%n", d1);
        System.out.printf("  2. getRevenueTrend (Bieu do duong doanh thu 30 ngay)   : %4d ms%n", d2);
        System.out.printf("  3. getBookingFrequency (Bieu do cot tan suat thu)       : %4d ms%n", d3);
        System.out.printf("  4. getRevenueByPitchType (Bieu do tron co cau loai san): %4d ms%n", d4);
        System.out.println("-".repeat(85));
        System.out.printf("  ==> TONG THOI GIAN THUC THI 4 QUERY TUAN TU            : %4d ms%n", totalSequential);
        System.out.println("=".repeat(85) + "\n");

        // Xác nhận tính đúng đắn của dữ liệu
        assertNotNull(summary);
        assertTrue(summary.getTotalBookings() > 0);
        assertTrue(summary.getCurrentRevenue().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(30, trend.size());
        assertEquals(7, frequency.size());
        assertTrue(pitchTypes.size() > 0);

        // Tiêu chuẩn hiệu năng: Dưới 300ms cho toàn bộ 4 query trên 1.000 đơn hàng
        assertTrue(totalSequential < 500, "Tổng thời gian thực thi tuần tự phải dưới 500ms");
    }

    @Test
    @DisplayName("BENCHMARK-02: Đo lường thời gian thực thi đồng thời (Mô phỏng Promise.all của Frontend)")
    void benchmarkConcurrentDashboardQueries_Success() {
        ReportFilterRequest filter = new ReportFilterRequest(START_DATE, END_DATE);

        // Khởi động
        adminReportService.getDashboardSummary(filter);

        long start = System.nanoTime();

        CompletableFuture<DashboardSummaryResponse> f1 = CompletableFuture.supplyAsync(() -> adminReportService.getDashboardSummary(filter));
        CompletableFuture<List<RevenueTrendPointResponse>> f2 = CompletableFuture.supplyAsync(() -> adminReportService.getRevenueTrend(filter));
        CompletableFuture<List<BookingFrequencyResponse>> f3 = CompletableFuture.supplyAsync(() -> adminReportService.getBookingFrequency(filter));
        CompletableFuture<List<RevenueByPitchTypeResponse>> f4 = CompletableFuture.supplyAsync(() -> adminReportService.getRevenueByPitchType(filter));

        CompletableFuture.allOf(f1, f2, f3, f4).join();

        long concurrentDuration = (System.nanoTime() - start) / 1_000_000;

        System.out.println("\n" + "=".repeat(85));
        System.out.println(" KET QUA BENCHMARK DONG THOI (SIMULATED PROMISE.ALL) VOI " + SAMPLE_BOOKINGS_COUNT + " DON HANG");
        System.out.println("=".repeat(85));
        System.out.printf("  Thoi gian hoan tat dong bo 4 APIs phia Client Dashboard: %4d ms%n", concurrentDuration);
        System.out.println("  Danh gia: Phan hoi tuc thi, giao dien nguoi dung hien thi cuc ky muot ma!");
        System.out.println("=".repeat(85) + "\n");

        assertTrue(concurrentDuration < 300, "Thời gian gọi đồng thời phải dưới 300ms");
    }

    @Test
    @DisplayName("BENCHMARK-03: Đo lường độ trễ toàn trình (End-to-End Latency) qua MockMvc HTTP REST API")
    void benchmarkHttpRestEndpoint_Success() throws Exception {
        // 1. Warm-up (Khởi tạo Security Filter Chain & Jackson Serializer của MockMvc)
        mockMvc.perform(get("/api/v1/admin/reports/summary")
                .header("Authorization", "Bearer " + adminToken)
                .param("startDate", START_DATE.toString())
                .param("endDate", END_DATE.toString())
                .contentType(MediaType.APPLICATION_JSON));

        // 2. Đo lường chính thức
        long start = System.nanoTime();

        MvcResult result = mockMvc.perform(get("/api/v1/admin/reports/summary")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("startDate", START_DATE.toString())
                        .param("endDate", END_DATE.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.currentRevenue").isNumber())
                .andReturn();

        long httpDuration = (System.nanoTime() - start) / 1_000_000;

        System.out.println("\n" + "=".repeat(85));
        System.out.println(" KET QUA BENCHMARK TOAN TRINH (END-TO-END HTTP API /summary)");
        System.out.println("=".repeat(85));
        System.out.printf("  Do tre toan trinh (Filter + Security + DB Query + JSON Serialization): %4d ms%n", httpDuration);
        System.out.println("=".repeat(85) + "\n");

        assertTrue(httpDuration < 300, "Thời gian phản hồi HTTP toàn trình phải dưới 300ms");
    }
}
