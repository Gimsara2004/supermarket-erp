package com.bci.productcrud.service;

import com.bci.productcrud.model.Payment;

import java.util.List;

public interface PaymentService {
    Payment create(Payment payment);
    List<Payment> findAll();
    Payment findById(Long id);
    void delete(Long id);
}
