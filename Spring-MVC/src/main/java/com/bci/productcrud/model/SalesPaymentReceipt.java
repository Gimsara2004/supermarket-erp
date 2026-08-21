package com.bci.productcrud.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales_payment_receipts")
public class SalesPaymentReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String receiptNumber;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cashier_id", nullable = false)
    private User cashier;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @PositiveOrZero
    @Column(nullable = false)
    private Double totalAmount = 0.0;

    @PositiveOrZero
    @Column(nullable = false)
    private Double discount = 0.0;

    private String paymentMethod;

    @Column(nullable = false)
    private String status = "OPEN";

    @NotEmpty
    @Valid
    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SalesReceiptItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "receipt")
    @JsonIgnore
    private List<Payment> payments = new ArrayList<>();

    @Column(updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    public SalesPaymentReceipt() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public User getCashier() {
        return cashier;
    }

    public void setCashier(User cashier) {
        this.cashier = cashier;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<SalesReceiptItem> getItems() {
        return items;
    }

    public void setItems(List<SalesReceiptItem> items) {
        this.items = items;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
