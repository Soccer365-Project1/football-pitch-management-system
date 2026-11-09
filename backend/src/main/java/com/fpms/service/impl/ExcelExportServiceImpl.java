package com.fpms.service.impl;

import com.fpms.dto.request.ReportFilterRequest;
import com.fpms.dto.response.report.BookingFrequencyResponse;
import com.fpms.dto.response.report.DashboardSummaryResponse;
import com.fpms.dto.response.report.RevenueByPitchTypeResponse;
import com.fpms.entity.Booking;
import com.fpms.entity.enums.BookingStatus;
import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.repository.BookingRepository;
import com.fpms.security.UserPrincipal;
import com.fpms.service.AdminReportService;
import com.fpms.service.ExcelExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelExportServiceImpl implements ExcelExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final byte[] NAVY_BLUE_RGB = new byte[]{(byte) 30, (byte) 64, (byte) 175}; // #1E40AF
    private static final byte[] LIGHT_GRAY_RGB = new byte[]{(byte) 243, (byte) 244, (byte) 246}; // #F3F4F6

    private final AdminReportService adminReportService;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional(readOnly = true)
    public byte[] exportRevenueReport(ReportFilterRequest filterRequest, UserPrincipal currentUser) {
        validateDateRange(filterRequest);

        LocalDate startDate = filterRequest.getStartDate();
        LocalDate endDate = filterRequest.getEndDate();

        List<Booking> bookings = bookingRepository.findBookingsForReportExport(startDate, endDate);
        if (bookings == null || bookings.isEmpty()) {
            throw new AppException(ErrorCode.REPORT_DATA_EMPTY);
        }

        DashboardSummaryResponse summary = adminReportService.getDashboardSummary(filterRequest);
        List<RevenueByPitchTypeResponse> pitchTypes = adminReportService.getRevenueByPitchType(filterRequest);
        List<BookingFrequencyResponse> frequencies = adminReportService.getBookingFrequency(filterRequest);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            StyleHolder styles = createStyles(workbook);

            // 1. Tạo Sheet 1: Tổng quan KPI & cơ cấu
            createKpiSummarySheet(workbook, styles, summary, pitchTypes, frequencies, filterRequest, currentUser);

            // 2. Tạo Sheet 2: Danh sách chi tiết đơn đặt sân
            createBookingDetailSheet(workbook, styles, bookings, filterRequest);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Lỗi I/O khi xuất file Excel báo cáo doanh thu: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.EXCEL_EXPORT_FAILED);
        } catch (AppException ae) {
            throw ae;
        } catch (Exception ex) {
            log.error("Lỗi không xác định khi xuất file Excel: {}", ex.getMessage(), ex);
            throw new AppException(ErrorCode.EXCEL_EXPORT_FAILED);
        }
    }

    private void validateDateRange(ReportFilterRequest request) {
        if (request == null || request.getStartDate() == null || request.getEndDate() == null) {
            return;
        }
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }
    }

    private void createKpiSummarySheet(XSSFWorkbook workbook,
                                      StyleHolder styles,
                                      DashboardSummaryResponse summary,
                                      List<RevenueByPitchTypeResponse> pitchTypes,
                                      List<BookingFrequencyResponse> frequencies,
                                      ReportFilterRequest filterRequest,
                                      UserPrincipal currentUser) {
        Sheet sheet = workbook.createSheet("TongQuan_KPI");
        sheet.setDisplayGridlines(true);

        int rowIdx = 0;

        // Dòng 0: Header thương hiệu
        Row brandRow = sheet.createRow(rowIdx++);
        Cell brandCell = brandRow.createCell(0);
        brandCell.setCellValue("HỆ THỐNG QUẢN LÝ SÂN BÓNG ĐÁ SOCCER365 (FPMS)");
        brandCell.setCellStyle(styles.brandStyle);

        // Dòng 1: Tiêu đề báo cáo
        Row titleRow = sheet.createRow(rowIdx++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("BÁO CÁO THỐNG KÊ DOANH THU & HIỆU SUẤT HOẠT ĐỘNG");
        titleCell.setCellStyle(styles.titleStyle);

        // Dòng 2: Khoảng thời gian kỳ báo cáo
        Row dateRow = sheet.createRow(rowIdx++);
        Cell dateCell = dateRow.createCell(0);
        dateCell.setCellValue(String.format("Kỳ báo cáo: Từ ngày %s đến ngày %s",
                filterRequest.getStartDate().format(DATE_FORMATTER),
                filterRequest.getEndDate().format(DATE_FORMATTER)));
        dateCell.setCellStyle(styles.subtitleStyle);

        // Dòng 3: Metadata xuất báo cáo
        Row metaRow = sheet.createRow(rowIdx++);
        Cell metaCell = metaRow.createCell(0);
        String exporter = currentUser != null && currentUser.getFullName() != null
                ? currentUser.getFullName()
                : "Quản trị viên";
        metaCell.setCellValue(String.format("Ngày xuất báo cáo: %s | Người xuất: %s",
                LocalDateTime.now().format(DATE_TIME_FORMATTER), exporter));
        metaCell.setCellStyle(styles.subtitleStyle);

        rowIdx++; // Dòng trống ngăn cách

        // ==================== I. CÁC CHỈ SỐ KPI CHÍNH ====================
        Row kpiSectionRow = sheet.createRow(rowIdx++);
        Cell kpiSectionCell = kpiSectionRow.createCell(0);
        kpiSectionCell.setCellValue("I. CÁC CHỈ SỐ KPI TỔNG HỢP");
        kpiSectionCell.setCellStyle(styles.sectionStyle);

        Row kpiHeaderRow = sheet.createRow(rowIdx++);
        String[] kpiHeaders = {"STT", "Chỉ Số Hoạt Động", "Giá Trị Kỳ Này", "So Sánh Kỳ Trước / Chi Tiết"};
        for (int i = 0; i < kpiHeaders.length; i++) {
            Cell cell = kpiHeaderRow.createCell(i);
            cell.setCellValue(kpiHeaders[i]);
            cell.setCellStyle(styles.headerStyle);
        }

        // 1. Doanh thu
        Row r1 = sheet.createRow(rowIdx++);
        r1.createCell(0).setCellValue(1);
        r1.getCell(0).setCellStyle(styles.centerStyle);
        r1.createCell(1).setCellValue("Doanh thu kỳ này");
        r1.getCell(1).setCellStyle(styles.textStyle);
        Cell cRev = r1.createCell(2);
        cRev.setCellValue(summary.getCurrentRevenue() != null ? summary.getCurrentRevenue().doubleValue() : 0.0);
        cRev.setCellStyle(styles.moneyStyle);
        Cell cGrowth = r1.createCell(3);
        double growth = summary.getRevenueGrowthRate() != null ? summary.getRevenueGrowthRate() : 0.0;
        cGrowth.setCellValue(String.format("%s%.1f%% so với kỳ trước", growth >= 0 ? "↑ +" : "↓ ", growth));
        cGrowth.setCellStyle(styles.textStyle);

        // 2. Tổng số đơn
        Row r2 = sheet.createRow(rowIdx++);
        r2.createCell(0).setCellValue(2);
        r2.getCell(0).setCellStyle(styles.centerStyle);
        r2.createCell(1).setCellValue("Tổng số đơn đặt sân");
        r2.getCell(1).setCellStyle(styles.textStyle);
        Cell cBookings = r2.createCell(2);
        cBookings.setCellValue(summary.getTotalBookings() != null ? summary.getTotalBookings() : 0);
        cBookings.setCellStyle(styles.centerStyle);
        Cell cBookingsDetail = r2.createCell(3);
        cBookingsDetail.setCellValue(String.format("Hoàn thành: %d | Đã xác nhận: %d",
                summary.getCompletedBookings() != null ? summary.getCompletedBookings() : 0,
                summary.getConfirmedBookings() != null ? summary.getConfirmedBookings() : 0));
        cBookingsDetail.setCellStyle(styles.textStyle);

        // 3. Tỉ lệ hủy đơn
        Row r3 = sheet.createRow(rowIdx++);
        r3.createCell(0).setCellValue(3);
        r3.getCell(0).setCellStyle(styles.centerStyle);
        r3.createCell(1).setCellValue("Tỉ lệ hủy đơn");
        r3.getCell(1).setCellStyle(styles.textStyle);
        Cell cCancel = r3.createCell(2);
        cCancel.setCellValue(String.format("%.1f%%", summary.getCancellationRate() != null ? summary.getCancellationRate() : 0.0));
        cCancel.setCellStyle(styles.centerStyle);
        Cell cCancelDiff = r3.createCell(3);
        double cancelDiff = summary.getCancellationRateDiff() != null ? summary.getCancellationRateDiff() : 0.0;
        cCancelDiff.setCellValue(String.format("%s%.1f%% so với kỳ trước", cancelDiff >= 0 ? "↑ +" : "↓ ", cancelDiff));
        cCancelDiff.setCellStyle(styles.textStyle);

        // 4. Lượt khách mới
        Row r4 = sheet.createRow(rowIdx++);
        r4.createCell(0).setCellValue(4);
        r4.getCell(0).setCellStyle(styles.centerStyle);
        r4.createCell(1).setCellValue("Lượt khách hàng mới");
        r4.getCell(1).setCellStyle(styles.textStyle);
        Cell cNewCust = r4.createCell(2);
        cNewCust.setCellValue(summary.getNewCustomersCount() != null ? summary.getNewCustomersCount() : 0);
        cNewCust.setCellStyle(styles.centerStyle);
        Cell cRepeat = r4.createCell(3);
        cRepeat.setCellValue(String.format("Tỉ lệ quay lại đặt sân: %.1f%%",
                summary.getRepeatCustomerRate() != null ? summary.getRepeatCustomerRate() : 0.0));
        cRepeat.setCellStyle(styles.textStyle);

        rowIdx++; // Dòng trống

        // ==================== II. CƠ CẤU DOANH THU THEO LOẠI SÂN ====================
        Row ptSectionRow = sheet.createRow(rowIdx++);
        Cell ptSectionCell = ptSectionRow.createCell(0);
        ptSectionCell.setCellValue("II. CƠ CẤU DOANH THU THEO PHÂN LOẠI SÂN");
        ptSectionCell.setCellStyle(styles.sectionStyle);

        Row ptHeaderRow = sheet.createRow(rowIdx++);
        String[] ptHeaders = {"Mã Loại Sân", "Tên Phân Loại Sân", "Doanh Thu (VNĐ)", "Tỷ Trọng (%)", "Số Lượt Đặt"};
        for (int i = 0; i < ptHeaders.length; i++) {
            Cell cell = ptHeaderRow.createCell(i);
            cell.setCellValue(ptHeaders[i]);
            cell.setCellStyle(styles.headerStyle);
        }

        if (pitchTypes != null && !pitchTypes.isEmpty()) {
            for (RevenueByPitchTypeResponse pt : pitchTypes) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(pt.getPitchTypeId() != null ? pt.getPitchTypeId() : 0);
                row.getCell(0).setCellStyle(styles.centerStyle);
                row.createCell(1).setCellValue(pt.getPitchTypeName() != null ? pt.getPitchTypeName() : "-");
                row.getCell(1).setCellStyle(styles.textStyle);
                Cell cRevPt = row.createCell(2);
                cRevPt.setCellValue(pt.getRevenue() != null ? pt.getRevenue().doubleValue() : 0.0);
                cRevPt.setCellStyle(styles.moneyStyle);
                Cell cPctPt = row.createCell(3);
                cPctPt.setCellValue(String.format("%.1f%%", pt.getPercentage() != null ? pt.getPercentage() : 0.0));
                cPctPt.setCellStyle(styles.centerStyle);
                Cell cCountPt = row.createCell(4);
                cCountPt.setCellValue(pt.getBookingCount() != null ? pt.getBookingCount() : 0);
                cCountPt.setCellStyle(styles.centerStyle);
            }
        }

        rowIdx++; // Dòng trống

        // ==================== III. TẦN SUẤT ĐẶT SÂN THEO THỨ ====================
        Row freqSectionRow = sheet.createRow(rowIdx++);
        Cell freqSectionCell = freqSectionRow.createCell(0);
        freqSectionCell.setCellValue("III. TẦN SUẤT ĐẶT SÂN THEO THỨ TRONG TUẦN (T2 - CHỦ NHẬT)");
        freqSectionCell.setCellStyle(styles.sectionStyle);

        Row freqHeaderRow = sheet.createRow(rowIdx++);
        String[] freqHeaders = {"Thứ Tự", "Thứ Trong Tuần", "Số Lượt Đặt Sân"};
        for (int i = 0; i < freqHeaders.length; i++) {
            Cell cell = freqHeaderRow.createCell(i);
            cell.setCellValue(freqHeaders[i]);
            cell.setCellStyle(styles.headerStyle);
        }

        if (frequencies != null && !frequencies.isEmpty()) {
            for (BookingFrequencyResponse freq : frequencies) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(freq.getDayOfWeekIndex());
                row.getCell(0).setCellStyle(styles.centerStyle);
                row.createCell(1).setCellValue(freq.getDayOfWeekName() != null ? freq.getDayOfWeekName() : "-");
                row.getCell(1).setCellStyle(styles.textStyle);
                row.createCell(2).setCellValue(freq.getBookingCount() != null ? freq.getBookingCount() : 0);
                row.getCell(2).setCellStyle(styles.centerStyle);
            }
        }

        for (int i = 0; i < 5; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1200, 4000));
        }
    }

    private void createBookingDetailSheet(XSSFWorkbook workbook,
                                         StyleHolder styles,
                                         List<Booking> bookings,
                                         ReportFilterRequest filterRequest) {
        Sheet sheet = workbook.createSheet("DanhSach_DonDatSan");
        sheet.setDisplayGridlines(true);

        int rowIdx = 0;

        // Dòng 0: Tiêu đề bảng
        Row titleRow = sheet.createRow(rowIdx++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("DANH SÁCH CHI TIẾT ĐƠN ĐẶT SÂN TRONG KỲ");
        titleCell.setCellStyle(styles.titleStyle);

        // Dòng 1: Phụ đề
        Row subRow = sheet.createRow(rowIdx++);
        Cell subCell = subRow.createCell(0);
        subCell.setCellValue(String.format("Kỳ báo cáo: Từ ngày %s đến %s | Tổng số: %d đơn đặt sân",
                filterRequest.getStartDate().format(DATE_FORMATTER),
                filterRequest.getEndDate().format(DATE_FORMATTER),
                bookings.size()));
        subCell.setCellStyle(styles.subtitleStyle);

        // Dòng 2: Header bảng
        Row headerRow = sheet.createRow(rowIdx++);
        String[] headers = {
                "STT", "Mã Đơn", "Tên Khách Hàng", "Số Điện Thoại",
                "Tên Sân", "Loại Sân", "Ngày Đá", "Khung Giờ",
                "Tiền Sân", "Tiền Cọc (30%)", "Tiền Còn Lại (70%)", "Phụ Phí",
                "Tổng Tiền", "Trạng Thái Đơn", "Thời Gian Tạo"
        };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.headerStyle);
        }

        int startDataRow = rowIdx + 1; // 1-indexed trong công thức Excel
        int currentStt = 1;

        for (Booking booking : bookings) {
            Row row = sheet.createRow(rowIdx++);

            // 0: STT
            Cell cStt = row.createCell(0);
            cStt.setCellValue(currentStt++);
            cStt.setCellStyle(styles.centerStyle);

            // 1: Mã đơn
            Cell cCode = row.createCell(1);
            cCode.setCellValue(booking.getBookingCode() != null ? booking.getBookingCode() : "-");
            cCode.setCellStyle(styles.centerStyle);

            // 2: Tên khách hàng
            String customerName = (booking.getCustomer() != null && booking.getCustomer().getFullName() != null)
                    ? booking.getCustomer().getFullName()
                    : (booking.getGuestName() != null ? booking.getGuestName() : "-");
            Cell cCustName = row.createCell(2);
            cCustName.setCellValue(customerName);
            cCustName.setCellStyle(styles.textStyle);

            // 3: SĐT
            String customerPhone = (booking.getCustomer() != null && booking.getCustomer().getPhoneNumber() != null)
                    ? booking.getCustomer().getPhoneNumber()
                    : (booking.getGuestPhone() != null ? booking.getGuestPhone() : "-");
            Cell cCustPhone = row.createCell(3);
            cCustPhone.setCellValue(customerPhone);
            cCustPhone.setCellStyle(styles.centerStyle);

            // 4: Tên sân
            String pitchName = booking.getPitchNameSnapshot() != null ? booking.getPitchNameSnapshot()
                    : (booking.getPitch() != null ? booking.getPitch().getName() : "-");
            Cell cPitch = row.createCell(4);
            cPitch.setCellValue(pitchName);
            cPitch.setCellStyle(styles.textStyle);

            // 5: Loại sân
            String pitchTypeName = (booking.getPitch() != null && booking.getPitch().getPitchType() != null)
                    ? booking.getPitch().getPitchType().getName() : "-";
            Cell cPitchType = row.createCell(5);
            cPitchType.setCellValue(pitchTypeName);
            cPitchType.setCellStyle(styles.centerStyle);

            // 6: Ngày đá
            Cell cDate = row.createCell(6);
            cDate.setCellValue(booking.getBookingDate() != null ? booking.getBookingDate().format(DATE_FORMATTER) : "-");
            cDate.setCellStyle(styles.centerStyle);

            // 7: Khung giờ
            String timeSlot = String.format("%s - %s",
                    booking.getStartTimeSnapshot() != null ? booking.getStartTimeSnapshot().toString() : "--:--",
                    booking.getEndTimeSnapshot() != null ? booking.getEndTimeSnapshot().toString() : "--:--");
            Cell cSlot = row.createCell(7);
            cSlot.setCellValue(timeSlot);
            cSlot.setCellStyle(styles.centerStyle);

            // 8: Tiền sân
            double pitchAmount = booking.getTotalPitchAmount() != null ? booking.getTotalPitchAmount().doubleValue() : 0.0;
            Cell cPitchAmt = row.createCell(8);
            cPitchAmt.setCellValue(pitchAmount);
            cPitchAmt.setCellStyle(styles.moneyStyle);

            // 9: Tiền cọc 30%
            double deposit = booking.getDepositAmount() != null ? booking.getDepositAmount().doubleValue() : 0.0;
            Cell cDep = row.createCell(9);
            cDep.setCellValue(deposit);
            cDep.setCellStyle(styles.moneyStyle);

            // 10: Tiền còn lại 70%
            double remaining = booking.getRemainingAmount() != null ? booking.getRemainingAmount().doubleValue() : 0.0;
            Cell cRem = row.createCell(10);
            cRem.setCellValue(remaining);
            cRem.setCellStyle(styles.moneyStyle);

            // 11: Phụ phí
            double fee = booking.getAdditionalFee() != null ? booking.getAdditionalFee().doubleValue() : 0.0;
            Cell cFee = row.createCell(11);
            cFee.setCellValue(fee);
            cFee.setCellStyle(styles.moneyStyle);

            // 12: Tổng tiền (Tiền sân + Phụ phí)
            double total = pitchAmount + fee;
            Cell cTot = row.createCell(12);
            cTot.setCellValue(total);
            cTot.setCellStyle(styles.moneyStyle);

            // 13: Trạng thái đơn (Tiếng Việt)
            Cell cStatus = row.createCell(13);
            cStatus.setCellValue(formatBookingStatus(booking.getStatus()));
            cStatus.setCellStyle(styles.centerStyle);

            // 14: Ngày tạo
            Cell cCreated = row.createCell(14);
            cCreated.setCellValue(booking.getCreatedAt() != null ? booking.getCreatedAt().format(DATE_TIME_FORMATTER) : "-");
            cCreated.setCellStyle(styles.centerStyle);
        }

        int endDataRow = rowIdx; // 1-indexed trong công thức Excel

        // ==================== DÒNG TỔNG CỘNG CUỐI BẢNG ====================
        Row totalRow = sheet.createRow(rowIdx++);
        Cell totalLabelCell = totalRow.createCell(0);
        totalLabelCell.setCellValue("TỔNG CỘNG");
        totalLabelCell.setCellStyle(styles.totalLabelStyle);

        for (int c = 1; c <= 7; c++) {
            Cell emptyCell = totalRow.createCell(c);
            emptyCell.setCellStyle(styles.totalLabelStyle);
        }
        sheet.addMergedRegion(new CellRangeAddress(totalRow.getRowNum(), totalRow.getRowNum(), 0, 7));

        // Cột 8: SUM Tiền sân (I)
        Cell sumPitch = totalRow.createCell(8);
        sumPitch.setCellFormula(String.format("SUM(I%d:I%d)", startDataRow, endDataRow));
        sumPitch.setCellStyle(styles.totalMoneyStyle);

        // Cột 9: SUM Tiền cọc (J)
        Cell sumDep = totalRow.createCell(9);
        sumDep.setCellFormula(String.format("SUM(J%d:J%d)", startDataRow, endDataRow));
        sumDep.setCellStyle(styles.totalMoneyStyle);

        // Cột 10: SUM Tiền còn lại (K)
        Cell sumRem = totalRow.createCell(10);
        sumRem.setCellFormula(String.format("SUM(K%d:K%d)", startDataRow, endDataRow));
        sumRem.setCellStyle(styles.totalMoneyStyle);

        // Cột 11: SUM Phụ phí (L)
        Cell sumFee = totalRow.createCell(11);
        sumFee.setCellFormula(String.format("SUM(L%d:L%d)", startDataRow, endDataRow));
        sumFee.setCellStyle(styles.totalMoneyStyle);

        // Cột 12: SUM Tổng tiền (M)
        Cell sumTotal = totalRow.createCell(12);
        sumTotal.setCellFormula(String.format("SUM(M%d:M%d)", startDataRow, endDataRow));
        sumTotal.setCellStyle(styles.totalMoneyStyle);

        Cell empty13 = totalRow.createCell(13);
        empty13.setCellStyle(styles.totalLabelStyle);
        Cell empty14 = totalRow.createCell(14);
        empty14.setCellStyle(styles.totalLabelStyle);

        // Cố định hàng tiêu đề bảng
        sheet.createFreezePane(0, 3);

        // Auto size các cột
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1200, 3500));
        }
    }

    private String formatBookingStatus(BookingStatus status) {
        if (status == null) return "-";
        return switch (status) {
            case PENDING_HOLD -> "Chờ giữ chỗ";
            case WAITING_APPROVAL -> "Chờ duyệt";
            case CONFIRMED -> "Đã xác nhận";
            case IN_PROGRESS -> "Đang diễn ra";
            case COMPLETED -> "Hoàn thành";
            case WAITING_CANCELLATION -> "Chờ duyệt hủy";
            case CANCELLED -> "Đã hủy";
            case REFUNDED -> "Đã hoàn tiền";
        };
    }

    private StyleHolder createStyles(XSSFWorkbook workbook) {
        DataFormat dataFormat = workbook.createDataFormat();

        // Colors
        XSSFColor navyColor = new XSSFColor(NAVY_BLUE_RGB, null);
        XSSFColor grayColor = new XSSFColor(LIGHT_GRAY_RGB, null);

        // Fonts
        XSSFFont brandFont = workbook.createFont();
        brandFont.setFontName("Arial");
        brandFont.setFontHeightInPoints((short) 13);
        brandFont.setBold(true);
        brandFont.setColor(navyColor);

        XSSFFont titleFont = workbook.createFont();
        titleFont.setFontName("Arial");
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setBold(true);

        XSSFFont subtitleFont = workbook.createFont();
        subtitleFont.setFontName("Arial");
        subtitleFont.setFontHeightInPoints((short) 10);
        subtitleFont.setItalic(true);

        XSSFFont sectionFont = workbook.createFont();
        sectionFont.setFontName("Arial");
        sectionFont.setFontHeightInPoints((short) 11);
        sectionFont.setBold(true);
        sectionFont.setColor(navyColor);

        XSSFFont headerFont = workbook.createFont();
        headerFont.setFontName("Arial");
        headerFont.setFontHeightInPoints((short) 10);
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        XSSFFont dataFont = workbook.createFont();
        dataFont.setFontName("Arial");
        dataFont.setFontHeightInPoints((short) 10);

        XSSFFont totalFont = workbook.createFont();
        totalFont.setFontName("Arial");
        totalFont.setFontHeightInPoints((short) 10);
        totalFont.setBold(true);

        // 1. Brand style
        XSSFCellStyle brandStyle = workbook.createCellStyle();
        brandStyle.setFont(brandFont);

        // 2. Title style
        XSSFCellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);

        // 3. Subtitle style
        XSSFCellStyle subtitleStyle = workbook.createCellStyle();
        subtitleStyle.setFont(subtitleFont);

        // 4. Section style
        XSSFCellStyle sectionStyle = workbook.createCellStyle();
        sectionStyle.setFont(sectionFont);

        // 5. Header table style (Navy blue background, white text)
        XSSFCellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(navyColor);
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(headerStyle);

        // 6. Text style (Left)
        XSSFCellStyle textStyle = workbook.createCellStyle();
        textStyle.setFont(dataFont);
        textStyle.setAlignment(HorizontalAlignment.LEFT);
        textStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(textStyle);

        // 7. Center style
        XSSFCellStyle centerStyle = workbook.createCellStyle();
        centerStyle.setFont(dataFont);
        centerStyle.setAlignment(HorizontalAlignment.CENTER);
        centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(centerStyle);

        // 8. Money style
        XSSFCellStyle moneyStyle = workbook.createCellStyle();
        moneyStyle.setFont(dataFont);
        moneyStyle.setAlignment(HorizontalAlignment.RIGHT);
        moneyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        moneyStyle.setDataFormat(dataFormat.getFormat("#,##0"));
        setBorders(moneyStyle);

        // 9. Total label style
        XSSFCellStyle totalLabelStyle = workbook.createCellStyle();
        totalLabelStyle.setFont(totalFont);
        totalLabelStyle.setFillForegroundColor(grayColor);
        totalLabelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        totalLabelStyle.setAlignment(HorizontalAlignment.CENTER);
        totalLabelStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(totalLabelStyle);

        // 10. Total money style
        XSSFCellStyle totalMoneyStyle = workbook.createCellStyle();
        totalMoneyStyle.setFont(totalFont);
        totalMoneyStyle.setFillForegroundColor(grayColor);
        totalMoneyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        totalMoneyStyle.setAlignment(HorizontalAlignment.RIGHT);
        totalMoneyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        totalMoneyStyle.setDataFormat(dataFormat.getFormat("#,##0"));
        setBorders(totalMoneyStyle);

        return new StyleHolder(brandStyle, titleStyle, subtitleStyle, sectionStyle,
                headerStyle, textStyle, centerStyle, moneyStyle, totalLabelStyle, totalMoneyStyle);
    }

    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private record StyleHolder(
            XSSFCellStyle brandStyle,
            XSSFCellStyle titleStyle,
            XSSFCellStyle subtitleStyle,
            XSSFCellStyle sectionStyle,
            XSSFCellStyle headerStyle,
            XSSFCellStyle textStyle,
            XSSFCellStyle centerStyle,
            XSSFCellStyle moneyStyle,
            XSSFCellStyle totalLabelStyle,
            XSSFCellStyle totalMoneyStyle
    ) {}
}
