package com.bci.productcrud.service;

import com.bci.productcrud.exception.PaymentNotFoundException;
import com.bci.productcrud.model.Payment;
import com.bci.productcrud.model.SalesReceipt;
import com.bci.productcrud.model.SalesReceiptStatus;
import com.bci.productcrud.repository.PaymentRepository;
import com.bci.productcrud.repository.SalesReceiptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final SalesReceiptService salesReceiptService;
    private final SalesReceiptRepository salesReceiptRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                               SalesReceiptService salesReceiptService,
                               SalesReceiptRepository salesReceiptRepository) {
        this.paymentRepository = paymentRepository;
        this.salesReceiptService = salesReceiptService;
        this.salesReceiptRepository = salesReceiptRepository;
    }

    @Override
    public Payment create(Payment request) {
        if (request.getSalesReceipt() == null || request.getSalesReceipt().getId() == null) {
            throw new IllegalArgumentException("A sales receipt (invoice) must be selected for the payment");
        }
        SalesReceipt receipt = salesReceiptService.findById(request.getSalesReceipt().getId());
        if (receipt.getStatus() == SalesReceiptStatus.VOIDED) {
            throw new IllegalArgumentException("Cannot record a payment against a voided sale");
        }

        Payment payment = new Payment();
        payment.setSalesReceipt(receipt);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setAmount(request.getAmount());
        Payment saved = paymentRepository.save(payment);

        double totalPaid = paymentRepository.sumAmountByReceiptId(receipt.getId());
        if (totalPaid >= receipt.getTotalAmount()) {
            receipt.setStatus(SalesReceiptStatus.PAID);
            salesReceiptRepository.save(receipt);
        }

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> findAll() {
        return paymentRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Payment findById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> findByReceipt(Long receiptId) {
        return paymentRepository.findBySalesReceiptId(receiptId);
    }

    @Override
    public void delete(Long id) {
        Payment payment = findById(id);
        SalesReceipt receipt = payment.getSalesReceipt();
        paymentRepository.delete(payment);

        double totalPaid = paymentRepository.sumAmountByReceiptId(receipt.getId());
        if (totalPaid < receipt.getTotalAmount() && receipt.getStatus() == SalesReceiptStatus.PAID) {
            receipt.setStatus(SalesReceiptStatus.OPEN);
            salesReceiptRepository.save(receipt);
        }
    }
}
