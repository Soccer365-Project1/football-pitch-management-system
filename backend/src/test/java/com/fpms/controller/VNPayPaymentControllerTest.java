package com.fpms.controller;

import com.fpms.dto.response.PaymentReturnResponse;
import com.fpms.exception.GlobalExceptionHandler;
import com.fpms.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Web Controller Test: VNPayPaymentController")
class VNPayPaymentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private VNPayPaymentController vnPayPaymentController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(vnPayPaymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/payments/vnpay/payment-return: Xử lý Return URL và trả về kết quả DTO")
    void testHandlePaymentReturn_Success() throws Exception {
        PaymentReturnResponse mockResponse = PaymentReturnResponse.builder()
                .status("SUCCESS")
                .bookingCode("#B123")
                .amount(new BigDecimal("90000"))
                .message("Đặt cọc thành công!")
                .build();

        when(paymentService.verifyAndProcessReturn(anyMap())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/payments/vnpay/payment-return")
                        .param("vnp_TxnRef", "TXN_001")
                        .param("vnp_ResponseCode", "00")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.bookingCode").value("#B123"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/vnpay/ipn: Nhận Webhook và trả về JSON chuẩn của VNPay")
    void testHandleVNPayIPN_Success() throws Exception {
        Map<String, String> ipnResult = new HashMap<>();
        ipnResult.put("RspCode", "00");
        ipnResult.put("Message", "Confirm Success");

        when(paymentService.processIPN(anyMap())).thenReturn(ipnResult);

        mockMvc.perform(get("/api/v1/payments/vnpay/ipn")
                        .param("vnp_TxnRef", "TXN_001")
                        .param("vnp_Amount", "9000000")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("00"))
                .andExpect(jsonPath("$.Message").value("Confirm Success"));
    }
}
