package com.fpms.controller;

import com.fpms.common.response.ApiResponse;
import com.fpms.dto.response.PaymentReturnResponse;
import com.fpms.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/vnpay")
@RequiredArgsConstructor
@Tag(name = "VNPay Payment", description = "Endpoints xử lý thanh toán và đối soát VNPay")
public class VNPayPaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Xử lý Return URL từ VNPay (Client Redirect)")
    @GetMapping("/payment-return")
    public ResponseEntity<ApiResponse<PaymentReturnResponse>> handlePaymentReturn(
            @RequestParam Map<String, String> queryParams
    ) {
        PaymentReturnResponse response = paymentService.verifyAndProcessReturn(queryParams);
        return ResponseEntity.ok(ApiResponse.success("Xử lý kết quả thanh toán", response));
    }

    @Operation(summary = "Xử lý IPN Webhook từ VNPay (Server-to-Server)")
    @GetMapping("/ipn")
    public ResponseEntity<Map<String, String>> handleVNPayIPN(
            @RequestParam Map<String, String> queryParams
    ) {
        Map<String, String> response = paymentService.processIPN(queryParams);
        return ResponseEntity.ok(response);
    }
}
