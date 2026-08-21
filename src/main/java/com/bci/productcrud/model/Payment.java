package com.bci.productcrud.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

/**
 * A single payment made against a SalesReceipt (invoice). A receipt can be
 * settled with more than one Payment (split payment), so this is a separate
 * child entity rather than a field on SalesReceipt.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "receipt_id", nullable = false)
    private SalesReceipt salesReceipt;

    @NotBlank
    @Column(nullable = false)
    private String paymentMethod;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Double amount;

    @Column(updatable = false)
    private Instant paidAt;

    public Payment() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SalesReceipt getSalesReceipt() {
        return salesReceipt;
    }

    public void setSalesReceipt(SalesReceipt salesReceipt) {
        this.salesReceipt = salesReceipt;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    @PrePersist
    void onCreate() {
        paidAt = Instant.now();
    }
}
