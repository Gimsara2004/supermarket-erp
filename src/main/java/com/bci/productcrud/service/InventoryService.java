package com.bci.productcrud.service;

import com.bci.productcrud.model.Inventory;

import java.util.List;

public interface InventoryService {

    List<Inventory> findAll();

    Inventory findById(Long id);

    List<Inventory> findByProduct(Long productId);

    Integer getTotalStock(Long productId);

    /** Creates or updates the inventory row for a product+location and sets its quantity directly (manual adjustment / stock take). */
    Inventory setQuantity(Long productId, Long locationId, Integer quantity);

    /** Adds (or subtracts, if negative) delta units of stock for a product at a location, creating the row if needed. */
    Inventory adjustQuantity(Long productId, Long locationId, int delta);

    void delete(Long id);
}
