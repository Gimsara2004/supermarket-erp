package com.bci.productcrud.service;

import com.bci.productcrud.model.SalesReceipt;

import java.util.List;

public interface SalesReceiptService {

    SalesReceipt create(SalesReceipt request);

    List<SalesReceipt> findAll();

    SalesReceipt findById(Long id);

    /** Voids a sale, reversing the stock deduction it caused (nothing else about a completed sale can be edited). */
    SalesReceipt voidReceipt(Long id);

    void delete(Long id);
}
