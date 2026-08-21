package com.bci.productcrud.service;

import com.bci.productcrud.exception.DuplicateReceiptNumberException;
import com.bci.productcrud.exception.SalesReceiptNotFoundException;
import com.bci.productcrud.model.Location;
import com.bci.productcrud.model.Product;
import com.bci.productcrud.model.SalesReceipt;
import com.bci.productcrud.model.SalesReceiptItem;
import com.bci.productcrud.model.SalesReceiptStatus;
import com.bci.productcrud.model.User;
import com.bci.productcrud.repository.SalesReceiptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SalesReceiptServiceImpl implements SalesReceiptService {

    private final SalesReceiptRepository salesReceiptRepository;
    private final ProductService productService;
    private final LocationService locationService;
    private final UserService userService;
    private final InventoryService inventoryService;

    public SalesReceiptServiceImpl(SalesReceiptRepository salesReceiptRepository,
                                    ProductService productService,
                                    LocationService locationService,
                                    UserService userService,
                                    InventoryService inventoryService) {
        this.salesReceiptRepository = salesReceiptRepository;
        this.productService = productService;
        this.locationService = locationService;
        this.userService = userService;
        this.inventoryService = inventoryService;
    }

    @Override
    public SalesReceipt create(SalesReceipt request) {
        if (request.getReceiptNumber() != null && salesReceiptRepository.existsByReceiptNumber(request.getReceiptNumber())) {
            throw new DuplicateReceiptNumberException("A sales receipt with number " + request.getReceiptNumber() + " already exists");
        }
        if (request.getCashier() == null || request.getCashier().getId() == null) {
            throw new IllegalArgumentException("A cashier must be selected for the sale");
        }
        if (request.getLocation() == null || request.getLocation().getId() == null) {
            throw new IllegalArgumentException("A location must be selected for the sale");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("A sale must contain at least one item");
        }

        User cashier = userService.findById(request.getCashier().getId());
        Location location = locationService.findById(request.getLocation().getId());

        SalesReceipt receipt = new SalesReceipt();
        receipt.setReceiptNumber(request.getReceiptNumber() == null || request.getReceiptNumber().isBlank()
                ? generateReceiptNumber() : request.getReceiptNumber());
        receipt.setCashier(cashier);
        receipt.setLocation(location);
        receipt.setDiscount(request.getDiscount() == null ? 0.0 : request.getDiscount());
        receipt.setPaymentMethod(request.getPaymentMethod());
        receipt.setStatus(SalesReceiptStatus.OPEN);

        double total = 0.0;
        for (SalesReceiptItem itemRequest : request.getItems()) {
            if (itemRequest.getProduct() == null || itemRequest.getProduct().getId() == null) {
                throw new IllegalArgumentException("Each sale line must reference a product");
            }
            Product product = productService.findById(itemRequest.getProduct().getId());

            // Will throw InsufficientStockException if there isn't enough stock at this location.
            inventoryService.adjustQuantity(product.getId(), location.getId(), -itemRequest.getQuantity());

            SalesReceiptItem item = new SalesReceiptItem();
            item.setProduct(product);
            item.setQuantity(itemRequest.getQuantity());
            item.setUnitPrice(itemRequest.getUnitPrice());
            item.setSubtotal(itemRequest.getQuantity() * itemRequest.getUnitPrice());
            item.setReceipt(receipt);
            receipt.getItems().add(item);

            total += item.getSubtotal();
        }

        receipt.setTotalAmount(Math.max(0, total - receipt.getDiscount()));

        return salesReceiptRepository.save(receipt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesReceipt> findAll() {
        return salesReceiptRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public SalesReceipt findById(Long id) {
        return salesReceiptRepository.findById(id)
                .orElseThrow(() -> new SalesReceiptNotFoundException("Sales receipt not found with id " + id));
    }

    @Override
    public SalesReceipt voidReceipt(Long id) {
        SalesReceipt receipt = findById(id);
        if (receipt.getStatus() == SalesReceiptStatus.VOIDED) {
            return receipt;
        }
        for (SalesReceiptItem item : receipt.getItems()) {
            inventoryService.adjustQuantity(item.getProduct().getId(), receipt.getLocation().getId(), item.getQuantity());
        }
        receipt.setStatus(SalesReceiptStatus.VOIDED);
        return salesReceiptRepository.save(receipt);
    }

    @Override
    public void delete(Long id) {
        SalesReceipt receipt = findById(id);
        if (receipt.getStatus() != SalesReceiptStatus.VOIDED) {
            // Reverse stock effect before hard-deleting, same as voiding.
            for (SalesReceiptItem item : receipt.getItems()) {
                inventoryService.adjustQuantity(item.getProduct().getId(), receipt.getLocation().getId(), item.getQuantity());
            }
        }
        salesReceiptRepository.delete(receipt);
    }

    private String generateReceiptNumber() {
        return "RCPT-" + System.currentTimeMillis();
    }
}
