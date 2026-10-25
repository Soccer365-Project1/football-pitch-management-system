package com.fpms.service;

import com.fpms.dto.request.ReportFilterRequest;
import com.fpms.dto.response.report.BookingFrequencyResponse;
import com.fpms.dto.response.report.DashboardSummaryResponse;
import com.fpms.dto.response.report.RevenueByPitchTypeResponse;
import com.fpms.entity.*;
import com.fpms.entity.enums.BookingStatus;
import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.repository.BookingRepository;
import com.fpms.security.UserPrincipal;
import com.fpms.service.AdminReportService;
import com.fpms.service.impl.ExcelExportServiceImpl;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExcelExportServiceTest {

    @Mock
    private AdminReportService adminReportService;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private ExcelExportServiceImpl excelExportService;

    private UserPrincipal testAdmin;
    private ReportFilterRequest validFilterRequest;

    @BeforeEach
    void setUp() {
        testAdmin = UserPrincipal.builder()
                .id(1L)
                .email("admin@soccer365.com")
                .fullName("Nguyễn Khánh Sơn (Admin)")
                .build();

        validFilterRequest = new ReportFilterRequest(
                LocalDate.of(2026, 8, 17),
                LocalDate.of(2026, 8, 23)
        );
    }

    @Test
    @DisplayName("exportRevenueReport: Thành công sinh file Excel 2 sheet có đầy đủ dữ liệu và định dạng")
    void exportRevenueReport_Success() throws IOException {
        // Given
        DashboardSummaryResponse mockSummary = DashboardSummaryResponse.builder()
                .currentRevenue(BigDecimal.valueOf(25000000))
                .previousRevenue(BigDecimal.valueOf(20000000))
                .revenueGrowthRate(25.0)
                .totalBookings(40L)
                .completedBookings(35L)
                .confirmedBookings(3L)
                .cancelledBookings(2L)
                .cancellationRate(5.0)
                .previousCancellationRate(6.0)
                .cancellationRateDiff(-1.0)
                .newCustomersCount(15L)
                .repeatCustomerRate(40.0)
                .build();

        List<RevenueByPitchTypeResponse> mockPitchTypes = List.of(
                RevenueByPitchTypeResponse.builder()
                        .pitchTypeId(1L)
                        .pitchTypeName("Sân 5 người")
                        .revenue(BigDecimal.valueOf(15000000))
                        .percentage(60.0)
                        .bookingCount(25L)
                        .build(),
                RevenueByPitchTypeResponse.builder()
                        .pitchTypeId(2L)
                        .pitchTypeName("Sân 7 người")
                        .revenue(BigDecimal.valueOf(10000000))
                        .percentage(40.0)
                        .bookingCount(15L)
                        .build()
        );

        List<BookingFrequencyResponse> mockFrequencies = List.of(
                BookingFrequencyResponse.builder().dayOfWeekIndex(1).dayOfWeekName("Thứ 2").bookingCount(5L).build(),
                BookingFrequencyResponse.builder().dayOfWeekIndex(7).dayOfWeekName("Chủ Nhật").bookingCount(12L).build()
        );

        User mockCustomer = User.builder()
                .fullName("Trần Văn Khách")
                .phoneNumber("0987654321")
                .build();

        PitchType pitchType5 = PitchType.builder().name("Sân 5 người").build();
        Pitch mockPitch = Pitch.builder().name("Sân A1").pitchType(pitchType5).build();
        TimeSlot mockTimeSlot = TimeSlot.builder().startTime(LocalTime.of(17, 30)).endTime(LocalTime.of(19, 0)).build();

        Booking mockBooking = Booking.builder()
                .bookingCode("BK20260817001")
                .customer(mockCustomer)
                .pitch(mockPitch)
                .pitchNameSnapshot("Sân A1")
                .timeSlot(mockTimeSlot)
                .startTimeSnapshot(LocalTime.of(17, 30))
                .endTimeSnapshot(LocalTime.of(19, 0))
                .bookingDate(LocalDate.of(2026, 8, 17))
                .totalPitchAmount(BigDecimal.valueOf(300000))
                .depositAmount(BigDecimal.valueOf(90000))
                .remainingAmount(BigDecimal.valueOf(210000))
                .additionalFee(BigDecimal.valueOf(20000))
                .status(BookingStatus.COMPLETED)
                .build();

        when(adminReportService.getDashboardSummary(validFilterRequest)).thenReturn(mockSummary);
        when(adminReportService.getRevenueByPitchType(validFilterRequest)).thenReturn(mockPitchTypes);
        when(adminReportService.getBookingFrequency(validFilterRequest)).thenReturn(mockFrequencies);
        when(bookingRepository.findBookingsForReportExport(validFilterRequest.getStartDate(), validFilterRequest.getEndDate()))
                .thenReturn(List.of(mockBooking));

        // When
        byte[] excelBytes = excelExportService.exportRevenueReport(validFilterRequest, testAdmin);

        // Then
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            assertEquals(2, workbook.getNumberOfSheets(), "Workbook phải có đúng 2 sheet");

            // Kiểm tra Sheet 1: Tổng Quan KPI
            Sheet kpiSheet = workbook.getSheet("TongQuan_KPI");
            assertNotNull(kpiSheet, "Sheet TongQuan_KPI phải tồn tại");
            Row titleRow = kpiSheet.getRow(1);
            assertNotNull(titleRow);
            assertTrue(titleRow.getCell(0).getStringCellValue().contains("BÁO CÁO THỐNG KÊ DOANH THU"));

            // Kiểm tra Sheet 2: Danh Sách Đơn Đặt Sân
            Sheet detailSheet = workbook.getSheet("DanhSach_DonDatSan");
            assertNotNull(detailSheet, "Sheet DanhSach_DonDatSan phải tồn tại");

            // Row 2 là Header
            Row headerRow = detailSheet.getRow(2);
            assertEquals("STT", headerRow.getCell(0).getStringCellValue());
            assertEquals("Mã Đơn", headerRow.getCell(1).getStringCellValue());
            assertEquals("Tên Khách Hàng", headerRow.getCell(2).getStringCellValue());

            // Row 3 là đơn hàng đầu tiên
            Row dataRow = detailSheet.getRow(3);
            assertEquals("BK20260817001", dataRow.getCell(1).getStringCellValue());
            assertEquals("Trần Văn Khách", dataRow.getCell(2).getStringCellValue());
            assertEquals("Sân A1", dataRow.getCell(4).getStringCellValue());
            assertEquals(300000.0, dataRow.getCell(8).getNumericCellValue()); // Tiền sân
            assertEquals(90000.0, dataRow.getCell(9).getNumericCellValue());  // Tiền cọc 30%
            assertEquals(210000.0, dataRow.getCell(10).getNumericCellValue()); // Tiền còn lại 70%
            assertEquals(20000.0, dataRow.getCell(11).getNumericCellValue()); // Phụ phí
            assertEquals(320000.0, dataRow.getCell(12).getNumericCellValue()); // Tổng tiền = 300k + 20k

            // Row 4 là Row Tổng Cộng có công thức SUM
            Row totalRow = detailSheet.getRow(4);
            assertNotNull(totalRow);
            assertTrue(totalRow.getCell(0).getStringCellValue().contains("TỔNG CỘNG"));
            assertEquals("SUM(I4:I4)", totalRow.getCell(8).getCellFormula());
        }
    }

    @Test
    @DisplayName("exportRevenueReport: Ném lỗi REPORT_DATA_EMPTY (5002) khi không có đơn nào trong kỳ (Luồng 4a SRS)")
    void exportRevenueReport_EmptyData_ThrowsException() {
        // Given
        when(bookingRepository.findBookingsForReportExport(validFilterRequest.getStartDate(), validFilterRequest.getEndDate()))
                .thenReturn(Collections.emptyList());

        // When & Then
        AppException exception = assertThrows(AppException.class, () ->
                excelExportService.exportRevenueReport(validFilterRequest, testAdmin));

        assertEquals(ErrorCode.REPORT_DATA_EMPTY, exception.getErrorCode());
        assertEquals(5002, exception.getErrorCode().getCode());
    }

    @Test
    @DisplayName("exportRevenueReport: Ném lỗi INVALID_DATE_RANGE (5001) khi ngày bắt đầu lớn hơn ngày kết thúc")
    void exportRevenueReport_InvalidDateRange_ThrowsException() {
        // Given
        ReportFilterRequest invalidRequest = new ReportFilterRequest(
                LocalDate.of(2026, 8, 25),
                LocalDate.of(2026, 8, 20)
        );

        // When & Then
        AppException exception = assertThrows(AppException.class, () ->
                excelExportService.exportRevenueReport(invalidRequest, testAdmin));

        assertEquals(ErrorCode.INVALID_DATE_RANGE, exception.getErrorCode());
    }
}
