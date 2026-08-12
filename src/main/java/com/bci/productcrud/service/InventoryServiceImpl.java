package com.bci.productcrud.service;

import com.bci.productcrud.exception.InsufficientStockException;
import com.bci.productcrud.model.Inventory;
import com.bci.productcrud.model.Location;
import com.bci.productcrud.model.Product;
import com.bci.productcrud.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryServiceImpl(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inventory> findAll() {
        return inventoryRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inventory> findByProduct(Long productId) {
        return inventoryRepository.findByProductId(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inventory> findByLocation(Long locationId) {
        return inventoryRepository.findByLocationId(locationId);
    }

    @Override
    public Inventory getOrCreate(Product product, Location location) {
        return inventoryRepository.findByProductIdAndLocationId(product.getId(), location.getId())
                .orElseGet(() -> {
                    Inventory inventory = new Inventory();
                    inventory.setProduct(product);
                    inventory.setLocation(location);
                    inventory.setQuantityOnHand(0);
                    return inventoryRepository.save(inventory);
                });
    }

    @Override
    public Inventory increase(Product product, Location location, int quantity) {
        Inventory inventory = getOrCreate(product, location);
        inventory.setQuantityOnHand(inventory.getQuantityOnHand() + quantity);
        return inventoryRepository.save(inventory);
    }

    @Override
    public Inventory decrease(Product product, Location location, int quantity) {
        Inventory inventory = getOrCreate(product, location);
        if (inventory.getQuantityOnHand() < quantity) {
            throw new InsufficientStockException(
                    "Not enough stock of \"" + product.getName() + "\" at " + location.getName() +
                            " - only " + inventory.getQuantityOnHand() + " available, " + quantity + " requested.");
        }
        inventory.setQuantityOnHand(inventory.getQuantityOnHand() - quantity);
        return inventoryRepository.save(inventory);
    }
}
