package com.bci.productcrud.repository;

import com.bci.productcrud.model.SalesPaymentReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalesPaymentReceiptRepository extends JpaRepository<SalesPaymentReceipt, Long> {
    Optional<SalesPaymentReceipt> findByReceiptNumber(String receiptNumber);
    boolean existsByReceiptNumber(String receiptNumber);
    boolean existsByCashierId(Long cashierId);
}
