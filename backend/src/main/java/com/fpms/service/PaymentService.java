package com.fpms.service;

import com.fpms.dto.response.PaymentReturnResponse;

import java.util.Map;

public interface PaymentService {
    PaymentReturnResponse verifyAndProcessReturn(Map<String, String> queryParams);
    Map<String, String> processIPN(Map<String, String> queryParams);
}
