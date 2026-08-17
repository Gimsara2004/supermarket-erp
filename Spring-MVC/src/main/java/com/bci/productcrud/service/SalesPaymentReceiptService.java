package com.bci.productcrud.service;

import com.bci.productcrud.model.SalesPaymentReceipt;

import java.util.List;

public interface SalesPaymentReceiptService {
    SalesPaymentReceipt create(SalesPaymentReceipt receipt);
    List<SalesPaymentReceipt> findAll();
    SalesPaymentReceipt findById(Long id);
    void delete(Long id);
}
