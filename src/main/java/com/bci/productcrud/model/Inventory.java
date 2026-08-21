package com.bci.productcrud.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

/**
 * Tracks how many units of a given Product are physically on hand at a given
 * Location (branch/store/warehouse). Replaces the old single "quantity" field
 * that used to live directly on Product.
 */
@Entity
@Table(name = "inventory", uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "location_id"}))
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Integer quantityOnHand = 0;

    private Instant lastUpdated;

    public Inventory() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Integer getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(Integer quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    @PrePersist
    @PreUpdate
    void onSave() {
        lastUpdated = Instant.now();
        if (quantityOnHand == null) {
            quantityOnHand = 0;
        }
    }
}
