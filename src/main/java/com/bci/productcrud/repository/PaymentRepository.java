package com.bci.productcrud.repository;

import com.bci.productcrud.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findBySalesReceiptId(Long salesReceiptId);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.salesReceipt.id = :receiptId")
    Double sumAmountByReceiptId(@Param("receiptId") Long receiptId);
}
