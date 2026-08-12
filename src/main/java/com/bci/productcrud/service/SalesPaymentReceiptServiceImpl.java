package com.bci.productcrud.service;

import com.bci.productcrud.exception.DuplicateReceiptNumberException;
import com.bci.productcrud.exception.SalesReceiptNotFoundException;
import com.bci.productcrud.model.Location;
import com.bci.productcrud.model.Product;
import com.bci.productcrud.model.SalesPaymentReceipt;
import com.bci.productcrud.model.SalesReceiptItem;
import com.bci.productcrud.model.User;
import com.bci.productcrud.repository.ProductRepository;
import com.bci.productcrud.repository.SalesPaymentReceiptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SalesPaymentReceiptServiceImpl implements SalesPaymentReceiptService {

    private final SalesPaymentReceiptRepository salesPaymentReceiptRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final LocationService locationService;
    private final UserService userService;
    private final InventoryService inventoryService;

    public SalesPaymentReceiptServiceImpl(SalesPaymentReceiptRepository salesPaymentReceiptRepository,
                                           ProductRepository productRepository,
                                           ProductService productService,
                                           LocationService locationService,
                                           UserService userService,
                                           InventoryService inventoryService) {
        this.salesPaymentReceiptRepository = salesPaymentReceiptRepository;
        this.productRepository = productRepository;
        this.productService = productService;
        this.locationService = locationService;
        this.userService = userService;
        this.inventoryService = inventoryService;
    }

    @Override
    public SalesPaymentReceipt create(SalesPaymentReceipt request) {
        if (salesPaymentReceiptRepository.existsByReceiptNumber(request.getReceiptNumber())) {
            throw new DuplicateReceiptNumberException("A sales receipt with number " + request.getReceiptNumber() + " already exists");
        }

        Location location = resolveLocation(request.getLocation());
        User cashier = resolveCashier(request.getCashier());

        SalesPaymentReceipt receipt = new SalesPaymentReceipt();
        receipt.setReceiptNumber(request.getReceiptNumber());
        receipt.setCashier(cashier);
        receipt.setLocation(location);
        receipt.setDiscount(request.getDiscount() != null ? request.getDiscount() : 0.0);
        receipt.setPaymentMethod(request.getPaymentMethod());
        receipt.setStatus("OPEN");

        double runningTotal = 0.0;
        for (SalesReceiptItem itemRequest : request.getItems()) {
            if (itemRequest.getProduct() == null || itemRequest.getProduct().getId() == null) {
                throw new IllegalArgumentException("Each sales receipt line must reference a product");
            }
            Product product = productService.findById(itemRequest.getProduct().getId());

            // Selling reduces stock - the reverse of a GRN. Checks and updates the
            // per-location Inventory ledger, and keeps Product's aggregate total in sync.
            inventoryService.decrease(product, location, itemRequest.getQuantity());
            product.setQuantity(Math.max(0, product.getQuantity() - itemRequest.getQuantity()));
            productRepository.save(product);

            SalesReceiptItem item = new SalesReceiptItem();
            item.setProduct(product);
            item.setQuantity(itemRequest.getQuantity());
            item.setUnitPrice(itemRequest.getUnitPrice());
            // Subtotal is calculated server-side rather than trusted from the client.
            double subtotal = itemRequest.getQuantity() * itemRequest.getUnitPrice();
            item.setSubtotal(subtotal);
            item.setReceipt(receipt);
            receipt.getItems().add(item);

            runningTotal += subtotal;
        }

        receipt.setTotalAmount(Math.max(0, runningTotal - receipt.getDiscount()));

        return salesPaymentReceiptRepository.save(receipt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesPaymentReceipt> findAll() {
        return salesPaymentReceiptRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public SalesPaymentReceipt findById(Long id) {
        return salesPaymentReceiptRepository.findById(id)
                .orElseThrow(() -> new SalesReceiptNotFoundException("Sales receipt not found with id " + id));
    }

    @Override
    public void delete(Long id) {
        SalesPaymentReceipt receipt = findById(id);
        // Reverse the stock effect this sale caused, same reversal pattern as GRN delete.
        for (SalesReceiptItem item : receipt.getItems()) {
            Product product = item.getProduct();
            inventoryService.increase(product, receipt.getLocation(), item.getQuantity());
            product.setQuantity(product.getQuantity() + item.getQuantity());
            productRepository.save(product);
        }
        salesPaymentReceiptRepository.delete(receipt);
    }

    private Location resolveLocation(Location requested) {
        if (requested == null || requested.getId() == null) {
            throw new IllegalArgumentException("A location must be selected for this sales receipt");
        }
        return locationService.findById(requested.getId());
    }

    private User resolveCashier(User requested) {
        if (requested == null || requested.getId() == null) {
            throw new IllegalArgumentException("A cashier must be selected for this sales receipt");
        }
        return userService.findById(requested.getId());
    }
}
