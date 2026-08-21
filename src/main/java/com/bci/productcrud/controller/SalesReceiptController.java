package com.bci.productcrud.controller;

import com.bci.productcrud.model.SalesReceipt;
import com.bci.productcrud.service.SalesReceiptService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales-receipts")
public class SalesReceiptController {

    private final SalesReceiptService salesReceiptService;

    public SalesReceiptController(SalesReceiptService salesReceiptService) {
        this.salesReceiptService = salesReceiptService;
    }

    @PostMapping
    public ResponseEntity<SalesReceipt> create(@Valid @RequestBody SalesReceipt receipt) {
        SalesReceipt created = salesReceiptService.create(receipt);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<SalesReceipt> findAll() {
        return salesReceiptService.findAll();
    }

    @GetMapping("/{id}")
    public SalesReceipt findById(@PathVariable Long id) {
        return salesReceiptService.findById(id);
    }

    @PutMapping("/{id}/void")
    public SalesReceipt voidReceipt(@PathVariable Long id) {
        return salesReceiptService.voidReceipt(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        salesReceiptService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
