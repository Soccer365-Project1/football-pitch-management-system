package com.fpms.service.impl;

import com.fpms.dto.request.TransactionRequest;
import com.fpms.dto.request.TransactionSearchRequest;
import com.fpms.common.response.PageResponse;
import com.fpms.dto.response.TransactionDetailResponse;
import com.fpms.dto.response.TransactionSummaryResponse;
import com.fpms.entity.Booking;
import com.fpms.entity.PaymentMethod;
import com.fpms.entity.PaymentTransaction;
import com.fpms.entity.User;
import com.fpms.entity.enums.PaymentType;
import com.fpms.entity.enums.TransactionStatus;
import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.mapper.PaymentTransactionMapper;
import com.fpms.repository.PaymentMethodRepository;
import com.fpms.repository.PaymentTransactionRepository;
import com.fpms.repository.specification.PaymentTransactionSpecification;
import com.fpms.service.PaymentTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentTransactionServiceImpl implements PaymentTransactionService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final byte[] FOREST_GREEN_RGB = new byte[]{(byte) 4, (byte) 120, (byte) 87}; // #047857
    private static final byte[] LIGHT_GRAY_RGB = new byte[]{(byte) 243, (byte) 244, (byte) 246}; // #F3F4F6

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentTransactionMapper paymentTransactionMapper;

    @Override
    @Transactional
    public PaymentTransaction createTransaction(TransactionRequest request) {
        if (request == null || request.getBooking() == null) {
            throw new IllegalArgumentException("Thông tin giao dịch và đơn đặt sân không được để trống");
        }

        Booking booking = request.getBooking();

        // Ưu tiên dùng paymentMethod đã truyền vào, nếu chưa có thì tìm theo paymentMethodCode
        PaymentMethod paymentMethod = request.getPaymentMethod();
        if (paymentMethod == null && request.getPaymentMethodCode() != null) {
            paymentMethod = paymentMethodRepository.findByCode(request.getPaymentMethodCode())
                    .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_METHOD_NOT_FOUND));
        }

        String txnCode = "TXN_" + System.currentTimeMillis() + "_" + booking.getId();
        BigDecimal amount = request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO;
        TransactionStatus status = request.getStatus() != null ? request.getStatus() : TransactionStatus.PENDING;

        PaymentTransaction transaction = PaymentTransaction.builder()
                .transactionCode(txnCode)
                .booking(booking)
                .paymentMethod(paymentMethod)
                .amount(amount)
                .paymentType(request.getPaymentType())
                .status(status)
                .processedBy(request.getProcessedBy())
                .note(request.getNote())
                .createdAt(LocalDateTime.now())
                .build();

        PaymentTransaction saved = paymentTransactionRepository.save(transaction);
        log.info("Đã tạo giao dịch thanh toán {}: Mã={}, Booking={}, Số tiền={}, Phương thức={}, Trạng thái={}",
                request.getPaymentType(), txnCode, booking.getBookingCode(), amount,
                paymentMethod != null ? paymentMethod.getCode() : null, status);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransactionSummaryResponse> getAllTransactions(TransactionSearchRequest request) {
        TransactionSearchRequest searchRequest = (request != null) ? request : new TransactionSearchRequest();

        // Validate ràng buộc: ngày bắt đầu <= ngày kết thúc
        if (searchRequest.getFromDate() != null && searchRequest.getToDate() != null
                && searchRequest.getFromDate().isAfter(searchRequest.getToDate())) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        Specification<PaymentTransaction> spec = PaymentTransactionSpecification.filterTransactions(searchRequest);
        Pageable pageable = searchRequest.toPageable("createdAt", Sort.Direction.DESC);

        Page<PaymentTransaction> page = paymentTransactionRepository.findAll(spec, pageable);
        Page<TransactionSummaryResponse> responsePage = page.map(paymentTransactionMapper::toSummaryResponse);

        return PageResponse.from(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionDetailResponse getTransactionDetail(Long id) {
        PaymentTransaction txn = paymentTransactionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_TRANSACTION_NOT_FOUND));

        return paymentTransactionMapper.toDetailResponse(txn);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportTransactionsExcel(TransactionSearchRequest request) {
        TransactionSearchRequest searchRequest = (request != null) ? request : new TransactionSearchRequest();

        if (searchRequest.getFromDate() != null && searchRequest.getToDate() != null
                && searchRequest.getFromDate().isAfter(searchRequest.getToDate())) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        Specification<PaymentTransaction> spec = PaymentTransactionSpecification.filterTransactions(searchRequest);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

        List<PaymentTransaction> transactions = paymentTransactionRepository.findAll(spec, sort);
        if (transactions == null || transactions.isEmpty()) {
            throw new AppException(ErrorCode.REPORT_DATA_EMPTY);
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            buildTransactionExcelSheet(workbook, transactions, searchRequest);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Lỗi I/O khi xuất file Excel danh sách giao dịch: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.EXCEL_EXPORT_FAILED);
        }
    }

    private void buildTransactionExcelSheet(XSSFWorkbook workbook, List<PaymentTransaction> transactions, TransactionSearchRequest request) {
        Sheet sheet = workbook.createSheet("Danh sách giao dịch");
        sheet.setDisplayGridlines(true);

        DataFormat dataFormat = workbook.createDataFormat();
        XSSFColor greenColor = new XSSFColor(FOREST_GREEN_RGB, null);
        XSSFColor grayColor = new XSSFColor(LIGHT_GRAY_RGB, null);

        // Fonts
        XSSFFont titleFont = workbook.createFont();
        titleFont.setFontName("Arial");
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setBold(true);
        titleFont.setColor(greenColor);

        XSSFFont subtitleFont = workbook.createFont();
        subtitleFont.setFontName("Arial");
        subtitleFont.setFontHeightInPoints((short) 10);
        subtitleFont.setItalic(true);

        XSSFFont headerFont = workbook.createFont();
        headerFont.setFontName("Arial");
        headerFont.setFontHeightInPoints((short) 10);
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        XSSFFont dataFont = workbook.createFont();
        dataFont.setFontName("Arial");
        dataFont.setFontHeightInPoints((short) 10);

        XSSFFont boldFont = workbook.createFont();
        boldFont.setFontName("Arial");
        boldFont.setFontHeightInPoints((short) 10);
        boldFont.setBold(true);

        // Styles
        XSSFCellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);

        XSSFCellStyle subtitleStyle = workbook.createCellStyle();
        subtitleStyle.setFont(subtitleFont);

        XSSFCellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(greenColor);
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        setAllBorders(headerStyle);

        XSSFCellStyle cellLeft = workbook.createCellStyle();
        cellLeft.setFont(dataFont);
        setAllBorders(cellLeft);

        XSSFCellStyle cellCenter = workbook.createCellStyle();
        cellCenter.setFont(dataFont);
        cellCenter.setAlignment(HorizontalAlignment.CENTER);
        setAllBorders(cellCenter);

        XSSFCellStyle cellBoldLeft = workbook.createCellStyle();
        cellBoldLeft.setFont(boldFont);
        setAllBorders(cellBoldLeft);

        XSSFCellStyle cellMoney = workbook.createCellStyle();
        cellMoney.setFont(boldFont);
        cellMoney.setAlignment(HorizontalAlignment.RIGHT);
        cellMoney.setDataFormat(dataFormat.getFormat("#,##0 \"₫\""));
        setAllBorders(cellMoney);

        XSSFCellStyle totalLabelStyle = workbook.createCellStyle();
        totalLabelStyle.setFont(boldFont);
        totalLabelStyle.setFillForegroundColor(grayColor);
        totalLabelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        totalLabelStyle.setAlignment(HorizontalAlignment.RIGHT);
        setAllBorders(totalLabelStyle);

        XSSFCellStyle totalMoneyStyle = workbook.createCellStyle();
        totalMoneyStyle.setFont(boldFont);
        totalMoneyStyle.setFillForegroundColor(grayColor);
        totalMoneyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        totalMoneyStyle.setAlignment(HorizontalAlignment.RIGHT);
        totalMoneyStyle.setDataFormat(dataFormat.getFormat("#,##0 \"₫\""));
        setAllBorders(totalMoneyStyle);

        // 1. Tiêu đề chính
        Row r0 = sheet.createRow(0);
        Cell c0 = r0.createCell(0);
        c0.setCellValue("DANH SÁCH GIAO DỊCH THANH TOÁN - SOCCER365");
        c0.setCellStyle(titleStyle);

        // 2. Phụ đề & ngày xuất
        Row r1 = sheet.createRow(1);
        Cell c1 = r1.createCell(0);
        c1.setCellValue("Thời gian xuất báo cáo: " + LocalDateTime.now().format(DATE_TIME_FORMATTER)
                + " | Tổng số bản ghi: " + transactions.size());
        c1.setCellStyle(subtitleStyle);

        // 3. Tiêu đề cột
        String[] headers = {
                "STT", "Mã Giao Dịch", "Mã Đơn Đặt", "Sân Bóng",
                "Khách Hàng", "Số Điện Thoại", "Loại Giao Dịch", "Phương Thức",
                "Số Tiền", "Trạng Thái", "Mã Đối Soát", "Người Xử Lý",
                "Thời Gian Tạo", "Ghi Chú"
        };

        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, headers.length - 1));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, headers.length - 1));

        int headerRowIndex = 3;
        Row headerRow = sheet.createRow(headerRowIndex);
        headerRow.setHeightInPoints(26);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 4. Đổ dữ liệu
        int currentRowIdx = 4;
        int stt = 1;
        for (PaymentTransaction txn : transactions) {
            Row row = sheet.createRow(currentRowIdx);
            row.setHeightInPoints(20);

            // STT
            Cell cellStt = row.createCell(0);
            cellStt.setCellValue(stt++);
            cellStt.setCellStyle(cellCenter);

            // Mã Giao Dịch
            Cell cellTxnCode = row.createCell(1);
            cellTxnCode.setCellValue(txn.getTransactionCode() != null ? txn.getTransactionCode() : "");
            cellTxnCode.setCellStyle(cellBoldLeft);

            // Mã Đơn Đặt
            Cell cellBookingCode = row.createCell(2);
            Booking booking = txn.getBooking();
            cellBookingCode.setCellValue(booking != null && booking.getBookingCode() != null ? booking.getBookingCode() : "");
            cellBookingCode.setCellStyle(cellBoldLeft);

            // Sân Bóng
            Cell cellPitch = row.createCell(3);
            cellPitch.setCellValue(booking != null && booking.getPitchNameSnapshot() != null ? booking.getPitchNameSnapshot() : "");
            cellPitch.setCellStyle(cellLeft);

            // Khách Hàng
            Cell cellCustomer = row.createCell(4);
            String customerName = "";
            String customerPhone = "";
            if (booking != null) {
                if (booking.getCustomer() != null && booking.getCustomer().getFullName() != null) {
                    customerName = booking.getCustomer().getFullName();
                } else if (booking.getGuestName() != null) {
                    customerName = booking.getGuestName();
                }

                if (booking.getCustomer() != null && booking.getCustomer().getPhoneNumber() != null) {
                    customerPhone = booking.getCustomer().getPhoneNumber();
                } else if (booking.getGuestPhone() != null) {
                    customerPhone = booking.getGuestPhone();
                }
            }
            cellCustomer.setCellValue(customerName);
            cellCustomer.setCellStyle(cellLeft);

            // SĐT
            Cell cellPhone = row.createCell(5);
            cellPhone.setCellValue(customerPhone);
            cellPhone.setCellStyle(cellCenter);

            // Loại Giao Dịch
            Cell cellType = row.createCell(6);
            cellType.setCellValue(formatPaymentType(txn.getPaymentType()));
            cellType.setCellStyle(cellCenter);

            // Phương Thức
            Cell cellMethod = row.createCell(7);
            String methodName = "";
            if (txn.getPaymentMethod() != null) {
                methodName = txn.getPaymentMethod().getName() != null ? txn.getPaymentMethod().getName() : txn.getPaymentMethod().getCode().name();
            }
            cellMethod.setCellValue(methodName);
            cellMethod.setCellStyle(cellLeft);

            // Số Tiền (VNĐ)
            Cell cellAmount = row.createCell(8);
            double amount = txn.getAmount() != null ? txn.getAmount().doubleValue() : 0.0;
            if (txn.getPaymentType() == PaymentType.REFUND) {
                amount = -amount;
            }
            cellAmount.setCellValue(amount);
            cellAmount.setCellStyle(cellMoney);

            // Trạng Thái
            Cell cellStatus = row.createCell(9);
            cellStatus.setCellValue(formatStatus(txn.getStatus()));
            cellStatus.setCellStyle(cellCenter);

            // Mã Đối Soát
            Cell cellRef = row.createCell(10);
            cellRef.setCellValue(txn.getReferenceCode() != null ? txn.getReferenceCode() : "");
            cellRef.setCellStyle(cellLeft);

            // Người Xử Lý
            Cell cellStaff = row.createCell(11);
            User staff = txn.getProcessedBy();
            cellStaff.setCellValue(staff != null && staff.getFullName() != null ? staff.getFullName() : "Online (Tự động)");
            cellStaff.setCellStyle(cellLeft);

            // Thời Gian Tạo
            Cell cellTime = row.createCell(12);
            cellTime.setCellValue(txn.getCreatedAt() != null ? txn.getCreatedAt().format(DATE_TIME_FORMATTER) : "");
            cellTime.setCellStyle(cellCenter);

            // Ghi Chú
            Cell cellNote = row.createCell(13);
            cellNote.setCellValue(txn.getNote() != null ? txn.getNote() : "");
            cellNote.setCellStyle(cellLeft);

            currentRowIdx++;
        }

        // 5. Hàng Tổng cộng
        Row totalRow = sheet.createRow(currentRowIdx);
        totalRow.setHeightInPoints(22);

        for (int i = 0; i < headers.length; i++) {
            Cell empty = totalRow.createCell(i);
            empty.setCellStyle(totalLabelStyle);
        }

        Cell totalLabelCell = totalRow.getCell(0);
        totalLabelCell.setCellValue("TỔNG CỘNG:");
        sheet.addMergedRegion(new CellRangeAddress(currentRowIdx, currentRowIdx, 0, 7));

        Cell sumAmount = totalRow.getCell(8);
        int startRowNumber = 5; // row index 4 in Excel is 5
        int endRowNumber = currentRowIdx; // 1-indexed
        sumAmount.setCellFormula(String.format("SUM(I%d:I%d)", startRowNumber, endRowNumber));
        sumAmount.setCellStyle(totalMoneyStyle);

        // 6. Cố định hàng tiêu đề bảng
        sheet.createFreezePane(0, 4);

        // 7. Auto size các cột (Cột STT để độ rộng cố định gọn gàng)
        sheet.setColumnWidth(0, 1800); // Khoảng 7 ký tự, vừa vặn cho "STT" và số thứ tự

        for (int i = 1; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1200, 3200));
        }
    }

    private String formatPaymentType(PaymentType type) {
        if (type == null) return "";
        return switch (type) {
            case DEPOSIT_30 -> "Cọc (30%)";
            case REMAINING_70 -> "Còn lại (70%)";
            case FULL_PAYMENT -> "Toàn bộ (100%)";
            case REFUND -> "Hoàn cọc";
        };
    }

    private String formatStatus(TransactionStatus status) {
        if (status == null) return "";
        return switch (status) {
            case SUCCESS -> "Thành công";
            case PENDING -> "Đang chờ xử lý";
            case FAILED -> "Thất bại";
        };
    }

    private void setAllBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
    }
}
