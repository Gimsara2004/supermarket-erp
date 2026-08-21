package com.bci.productcrud.repository;

import com.bci.productcrud.model.SalesReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalesReceiptRepository extends JpaRepository<SalesReceipt, Long> {

    Optional<SalesReceipt> findByReceiptNumber(String receiptNumber);

    boolean existsByReceiptNumber(String receiptNumber);
}
