package com.bci.productcrud.service;

import com.bci.productcrud.exception.OverpaymentException;
import com.bci.productcrud.exception.PaymentNotFoundException;
import com.bci.productcrud.model.Payment;
import com.bci.productcrud.model.SalesPaymentReceipt;
import com.bci.productcrud.repository.PaymentRepository;
import com.bci.productcrud.repository.SalesPaymentReceiptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final SalesPaymentReceiptRepository salesPaymentReceiptRepository;
    private final SalesPaymentReceiptService salesPaymentReceiptService;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                               SalesPaymentReceiptRepository salesPaymentReceiptRepository,
                               SalesPaymentReceiptService salesPaymentReceiptService) {
        this.paymentRepository = paymentRepository;
        this.salesPaymentReceiptRepository = salesPaymentReceiptRepository;
        this.salesPaymentReceiptService = salesPaymentReceiptService;
    }

    @Override
    public Payment create(Payment request) {
        if (request.getReceipt() == null || request.getReceipt().getId() == null) {
            throw new IllegalArgumentException("A sales receipt must be selected for this payment");
        }
        SalesPaymentReceipt receipt = salesPaymentReceiptService.findById(request.getReceipt().getId());

        double alreadyPaid = paymentRepository.findByReceiptId(receipt.getId()).stream()
                .mapToDouble(Payment::getAmount)
                .sum();

        if (alreadyPaid + request.getAmount() > receipt.getTotalAmount() + 0.001) {
            double remaining = receipt.getTotalAmount() - alreadyPaid;
            throw new OverpaymentException(
                    "Payment of " + request.getAmount() + " exceeds the remaining balance of " +
                            String.format("%.2f", remaining) + " on receipt " + receipt.getReceiptNumber() + ".");
        }

        Payment payment = new Payment();
        payment.setReceipt(receipt);
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setReference(request.getReference());

        Payment saved = paymentRepository.save(payment);

        if (alreadyPaid + request.getAmount() >= receipt.getTotalAmount() - 0.001) {
            receipt.setStatus("PAID");
            salesPaymentReceiptRepository.save(receipt);
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
    public void delete(Long id) {
        Payment payment = findById(id);
        SalesPaymentReceipt receipt = payment.getReceipt();
        paymentRepository.delete(payment);
        if ("PAID".equals(receipt.getStatus())) {
            receipt.setStatus("OPEN");
            salesPaymentReceiptRepository.save(receipt);
        }
    }
}
