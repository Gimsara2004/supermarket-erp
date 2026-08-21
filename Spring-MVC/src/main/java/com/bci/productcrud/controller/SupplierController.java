package com.bci.productcrud.controller;

import com.bci.productcrud.model.Supplier;
import com.bci.productcrud.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    public ResponseEntity<Supplier> create(@Valid @RequestBody Supplier supplier) {
        Supplier created = supplierService.create(supplier);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<Supplier> findAll() {
        return supplierService.findAll();
    }

    @GetMapping("/{id}")
    public Supplier findById(@PathVariable Long id) {
        return supplierService.findById(id);
    }

    @PutMapping("/{id}")
    public Supplier update(@PathVariable Long id, @Valid @RequestBody Supplier supplier) {
        return supplierService.update(id, supplier);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
