package com.bci.productcrud.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String barcode;

    @NotBlank
    @Column(nullable = false)
    private String name;

    private String description;

    /** Free-text grouping, e.g. "Beverages", "Dairy", "Household". */
    private String category;

    private String brand;

    /** e.g. "500ml", "1kg", "L", "XL" - kept as free text since units vary by product type. */
    private String size;

    private String color;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Double purchasePrice;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Double sellingPrice;

    /** ACTIVE / DISCONTINUED / OUT_OF_STOCK etc - kept as simple free text. */
    @Column(nullable = false)
    private String status = "ACTIVE";

    /**
     * Stock is no longer stored directly on Product - it now lives in the
     * Inventory table, split out per Location (branch/store/warehouse).
     * This is a read-only, computed sum across all locations, populated by
     * ProductService for display purposes only; it is never persisted.
     */
    @Transient
    private Integer totalStock = 0;

    @Column(updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    public Product() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Double getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(Double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public Double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(Double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status == null ? "ACTIVE" : status;
    }

    public Integer getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(Integer totalStock) {
        this.totalStock = totalStock;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
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
