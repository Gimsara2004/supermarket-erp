package com.bci.productcrud.service;

import com.bci.productcrud.model.PurchaseOrder;

import java.util.List;

public interface PurchaseOrderService {
    PurchaseOrder create(PurchaseOrder purchaseOrder);
    List<PurchaseOrder> findAll();
    PurchaseOrder findById(Long id);
    PurchaseOrder update(Long id, PurchaseOrder purchaseOrder);
    void delete(Long id);
}
