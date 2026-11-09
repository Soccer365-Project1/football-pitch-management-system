package com.fpms.service;

import com.fpms.dto.request.TransactionRequest;
import com.fpms.entity.PaymentTransaction;

public interface PaymentTransactionService {

    PaymentTransaction createTransaction(TransactionRequest request);
}
