package com.fpms.controller;

import com.fpms.common.response.ApiResponse;
import com.fpms.common.response.PageResponse;
import com.fpms.dto.request.TransactionSearchRequest;
import com.fpms.dto.response.TransactionDetailResponse;
import com.fpms.dto.response.TransactionSummaryResponse;
import com.fpms.service.PaymentTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Tag(name = "Admin Transaction Operations", description = "Các API Phân hệ Quản lý giao dịch thanh toán dành cho Admin & Staff")
@RestController
@RequestMapping("/api/v1/admin/transactions")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AdminTransactionController {

    private final PaymentTransactionService paymentTransactionService;

    @Operation(summary = "Lấy danh sách giao dịch thanh toán phân trang",
            description = "Hỗ trợ lọc theo trạng thái, loại thanh toán, phương thức, khoảng ngày, khoảng tiền và tìm kiếm từ khóa")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TransactionSummaryResponse>>> getTransactions(
            @Valid @ModelAttribute TransactionSearchRequest request) {
        PageResponse<TransactionSummaryResponse> response = paymentTransactionService.getAllTransactions(request);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách giao dịch thành công", response));
    }

    @Operation(summary = "Xem chi tiết một giao dịch thanh toán",
            description = "Lấy toàn diện thông tin giao dịch, mã đối soát ngân hàng, nhân viên xử lý và thông tin đơn đặt sân liên kết")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionDetailResponse>> getTransactionDetail(@PathVariable Long id) {
        TransactionDetailResponse response = paymentTransactionService.getTransactionDetail(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết giao dịch thành công", response));
    }

    @Operation(summary = "Xuất danh sách giao dịch ra file Excel (.xlsx)",
            description = "Áp dụng toàn bộ bộ lọc hiện tại và bỏ phân trang để xuất toàn bộ giao dịch thỏa mãn")
    @GetMapping("/export-excel")
    public ResponseEntity<byte[]> exportTransactionsExcel(@Valid @ModelAttribute TransactionSearchRequest request) {
        byte[] excelBytes = paymentTransactionService.exportTransactionsExcel(request);
        String fileName = String.format("GiaoDich_%s.xlsx", LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }
}

