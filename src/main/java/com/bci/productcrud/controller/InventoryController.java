package com.bci.productcrud.controller;

import com.bci.productcrud.model.Inventory;
import com.bci.productcrud.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/{id}")
    public Inventory findById(@PathVariable Long id) {
        return inventoryService.findById(id);
    }

    @GetMapping("/product/{productId}")
    public List<Inventory> findByProduct(@PathVariable Long productId) {
        return inventoryService.findByProduct(productId);
    }

    /** Manual stock-take / correction: set the exact quantity on hand for a product at a location. */
    @PutMapping("/product/{productId}/location/{locationId}")
    public Inventory setQuantity(@PathVariable Long productId, @PathVariable Long locationId,
                                  @RequestBody Map<String, Integer> body) {
        return inventoryService.setQuantity(productId, locationId, body.get("quantityOnHand"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        inventoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
