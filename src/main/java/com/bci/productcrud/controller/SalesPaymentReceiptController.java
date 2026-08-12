package com.bci.productcrud.controller;

import com.bci.productcrud.model.SalesPaymentReceipt;
import com.bci.productcrud.service.SalesPaymentReceiptService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales-receipts")
public class SalesPaymentReceiptController {

    private final SalesPaymentReceiptService salesPaymentReceiptService;

    public SalesPaymentReceiptController(SalesPaymentReceiptService salesPaymentReceiptService) {
        this.salesPaymentReceiptService = salesPaymentReceiptService;
    }

    @PostMapping
    public ResponseEntity<SalesPaymentReceipt> create(@Valid @RequestBody SalesPaymentReceipt receipt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salesPaymentReceiptService.create(receipt));
    }

    @GetMapping
    public List<SalesPaymentReceipt> findAll() {
        return salesPaymentReceiptService.findAll();
    }

    @GetMapping("/{id}")
    public SalesPaymentReceipt findById(@PathVariable Long id) {
        return salesPaymentReceiptService.findById(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        salesPaymentReceiptService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
