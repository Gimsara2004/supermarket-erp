package com.bci.productcrud.controller;

import com.bci.productcrud.model.Inventory;
import com.bci.productcrud.service.InventoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Read-only on purpose: Inventory quantities are only ever changed as a side
// effect of a GRN (stock in) or a Sales Receipt (stock out), never edited directly.
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<Inventory> findAll() {
        return inventoryService.findAll();
    }

    @GetMapping("/product/{productId}")
    public List<Inventory> findByProduct(@PathVariable Long productId) {
        return inventoryService.findByProduct(productId);
    }

    @GetMapping("/location/{locationId}")
    public List<Inventory> findByLocation(@PathVariable Long locationId) {
        return inventoryService.findByLocation(locationId);
    }
}
