package com.bci.productcrud.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "grn_items")
public class GrnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "grn_id")
    @JsonIgnore
    private GoodsReceivedNote grn;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "purchase_order_item_id", nullable = false)
    private PurchaseOrderItem purchaseOrderItem;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer receivedQuantity;

    public GrnItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GoodsReceivedNote getGrn() {
        return grn;
    }

    public void setGrn(GoodsReceivedNote grn) {
        this.grn = grn;
    }

    public PurchaseOrderItem getPurchaseOrderItem() {
        return purchaseOrderItem;
    }

    public void setPurchaseOrderItem(PurchaseOrderItem purchaseOrderItem) {
        this.purchaseOrderItem = purchaseOrderItem;
    }

    public Integer getReceivedQuantity() {
        return receivedQuantity;
    }

    public void setReceivedQuantity(Integer receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }
}
