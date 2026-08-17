package com.bci.productcrud.controller;

import com.bci.productcrud.model.PurchaseOrder;
import com.bci.productcrud.service.PurchaseOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @PostMapping
    public ResponseEntity<PurchaseOrder> create(@Valid @RequestBody PurchaseOrder purchaseOrder) {
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseOrderService.create(purchaseOrder));
    }

    @GetMapping
    public List<PurchaseOrder> findAll() {
        return purchaseOrderService.findAll();
    }

    @GetMapping("/{id}")
    public PurchaseOrder findById(@PathVariable Long id) {
        return purchaseOrderService.findById(id);
    }

    @PutMapping("/{id}")
    public PurchaseOrder update(@PathVariable Long id, @Valid @RequestBody PurchaseOrder purchaseOrder) {
        return purchaseOrderService.update(id, purchaseOrder);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        purchaseOrderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
