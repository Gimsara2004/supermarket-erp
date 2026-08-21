package com.bci.productcrud.service;

import com.bci.productcrud.model.Inventory;
import com.bci.productcrud.model.Location;
import com.bci.productcrud.model.Product;

import java.util.List;

public interface InventoryService {
    List<Inventory> findAll();
    List<Inventory> findByProduct(Long productId);
    List<Inventory> findByLocation(Long locationId);

    /** Finds (or creates with 0 stock) the ledger row for this product+location. */
    Inventory getOrCreate(Product product, Location location);

    /** Adds to stock at a location (used when goods are received). */
    Inventory increase(Product product, Location location, int quantity);

    /** Removes from stock at a location (used when goods are sold); throws if not enough stock. */
    Inventory decrease(Product product, Location location, int quantity);
}
