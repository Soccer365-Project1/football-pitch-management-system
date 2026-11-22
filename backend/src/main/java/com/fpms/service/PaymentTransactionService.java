package com.fpms.service;

import com.fpms.dto.request.TransactionRequest;
import com.fpms.dto.request.TransactionSearchRequest;
import com.fpms.common.response.PageResponse;
import com.fpms.dto.response.TransactionDetailResponse;
import com.fpms.dto.response.TransactionSummaryResponse;
import com.fpms.entity.PaymentTransaction;

public interface PaymentTransactionService {

    PaymentTransaction createTransaction(TransactionRequest request);

    PageResponse<TransactionSummaryResponse> getAllTransactions(TransactionSearchRequest request);

    TransactionDetailResponse getTransactionDetail(Long id);

    byte[] exportTransactionsExcel(TransactionSearchRequest request);
}
