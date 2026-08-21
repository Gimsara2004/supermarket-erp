package com.bci.productcrud.service;

import com.bci.productcrud.exception.InsufficientStockException;
import com.bci.productcrud.exception.InventoryNotFoundException;
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
    private final ProductService productService;
    private final LocationService locationService;

    public InventoryServiceImpl(InventoryRepository inventoryRepository,
                                 ProductService productService,
                                 LocationService locationService) {
        this.inventoryRepository = inventoryRepository;
        this.productService = productService;
        this.locationService = locationService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inventory> findAll() {
        return inventoryRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Inventory findById(Long id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory record not found with id " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inventory> findByProduct(Long productId) {
        return inventoryRepository.findByProductId(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalStock(Long productId) {
        Integer sum = inventoryRepository.sumQuantityByProductId(productId);
        return sum == null ? 0 : sum;
    }

    @Override
    public Inventory setQuantity(Long productId, Long locationId, Integer quantity) {
        Inventory inventory = findOrCreate(productId, locationId);
        inventory.setQuantityOnHand(quantity == null ? 0 : quantity);
        return inventoryRepository.save(inventory);
    }

    @Override
    public Inventory adjustQuantity(Long productId, Long locationId, int delta) {
        Inventory inventory = findOrCreate(productId, locationId);
        int newQuantity = inventory.getQuantityOnHand() + delta;
        if (newQuantity < 0) {
            throw new InsufficientStockException(
                    "Not enough stock of \"" + inventory.getProduct().getName() + "\" at " +
                            inventory.getLocation().getLocationName() + " (have " + inventory.getQuantityOnHand() +
                            ", need " + (-delta) + " more).");
        }
        inventory.setQuantityOnHand(newQuantity);
        return inventoryRepository.save(inventory);
    }

    @Override
    public void delete(Long id) {
        Inventory inventory = findById(id);
        inventoryRepository.delete(inventory);
    }

    private Inventory findOrCreate(Long productId, Long locationId) {
        return inventoryRepository.findByProductIdAndLocationId(productId, locationId)
                .orElseGet(() -> {
                    Product product = productService.findById(productId);
                    Location location = locationService.findById(locationId);
                    Inventory inventory = new Inventory();
                    inventory.setProduct(product);
                    inventory.setLocation(location);
                    inventory.setQuantityOnHand(0);
                    return inventory;
                });
    }
}
